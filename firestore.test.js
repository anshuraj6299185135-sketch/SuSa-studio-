const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function validProject(uid) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    title: "Neon Tokyo Launch Film",
    resolution: "4K 60fps",
    aspectRatio: "16:9",
    durationSeconds: 95,
    status: "editing",
    lutPreset: "Obsidian Cinema",
    shootLocationName: "Shibuya Crossing",
    shootLat: 35.6595,
    shootLng: 139.7004,
    collaboratorCount: 3,
    proxyEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validClip(uid) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    trackType: "video",
    label: "A-Roll Drone Intro",
    startMs: 0,
    durationMs: 12000,
    speedMultiplier: 1.5,
    colorHex: "#6366F1",
    mediaUri: "",
    notes: "Speed ramp 1.5x -> 1.0x",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validLocation(uid) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    name: "Shibuya Sky Rooftop",
    category: "Exterior",
    latitude: 35.6584,
    longitude: 139.7022,
    scoutingNotes: "Blue hour 18:10, wide 24mm",
    projectId: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated caller cannot read or write projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProject(ALICE_UID))
  );
});

test("Owner can create, read, update, and delete own StudioProject", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  await assertSucceeds(ref.set(validProject(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(ref.update({ title: "Updated Film", updatedAt: pastTimestamp() }));
  await assertSucceeds(ref.delete());
});

test("Cross-user access to projects and locations is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProject(ALICE_UID))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("locations").doc("loc_1").set(validLocation(ALICE_UID))
  );
});

test("Shadow update with ghost field or identity spoofing is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  await assertFails(ref.set({ ...validProject(ALICE_UID), isAdmin: true }));
  await assertFails(ref.set(validProject(BOB_UID)));
});

test("Clip creation requires existing parent project (Parent Gate)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orphanClipRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects")
    .doc("missing_proj")
    .collection("clips")
    .doc("clip_1");
  await assertFails(orphanClipRef.set(validClip(ALICE_UID)));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProject(ALICE_UID))
  );
  const validClipRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects")
    .doc("proj_1")
    .collection("clips")
    .doc("clip_1");
  await assertSucceeds(validClipRef.set(validClip(ALICE_UID)));
});

test("RenderJob enforces terminal state lock after completed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProject(ALICE_UID))
  );
  const jobRef = aliceDb.collection("users").doc(ALICE_UID).collection("renderJobs").doc("job_1");
  const ts = pastTimestamp();
  await assertSucceeds(
    jobRef.set({
      userId: ALICE_UID,
      projectId: "proj_1",
      projectTitle: "Neon Tokyo Launch Film",
      profile: "YouTube 4K H.265",
      status: "rendering",
      progressPercent: 45,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(jobRef.update({ status: "completed", progressPercent: 100, updatedAt: pastTimestamp() }));
  await assertFails(jobRef.update({ status: "rendering", progressPercent: 50, updatedAt: pastTimestamp() }));
});

test("SecurityAuditLog is append-only (no update or delete)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      eventType: "SIGN_IN",
      deviceLabel: "Android Studio Device",
      detail: "Authenticated via Google Credential Manager",
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(logRef.update({ detail: "Tampered" }));
  await assertFails(logRef.delete());
});

test("Personal AI chat threads and messages enforce ownership, schema, and Parent Gate", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  // Orphan message without thread fails Parent Gate
  const orphanMsgRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("aiThreads")
    .doc("missing_thread")
    .collection("messages")
    .doc("msg_1");
  await assertFails(
    orphanMsgRef.set({
      userId: ALICE_UID,
      role: "user",
      text: "Hello AI",
      hasImage: false,
      modelName: "gemini-3.8-flash",
      createdAt: ts,
    })
  );

  // Owner creates thread
  const threadRef = aliceDb.collection("users").doc(ALICE_UID).collection("aiThreads").doc("thread_1");
  await assertSucceeds(
    threadRef.set({
      userId: ALICE_UID,
      title: "Morning Brainstorm",
      modelName: "gemini-3.8-flash",
      systemInstruction: "Be concise.",
      lastMessagePreview: "Hello AI",
      messageCount: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid role rejected
  const msgRef = threadRef.collection("messages").doc("msg_1");
  await assertFails(
    msgRef.set({
      userId: ALICE_UID,
      role: "system_admin",
      text: "Hello AI",
      hasImage: false,
      modelName: "gemini-3.8-flash",
      createdAt: ts,
    })
  );

  // Valid user and model messages succeed
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      role: "user",
      text: "Hello AI",
      hasImage: false,
      modelName: "gemini-3.8-flash",
      createdAt: ts,
    })
  );

  // Message update is blocked, cross-user read is blocked
  await assertFails(msgRef.update({ text: "Edited" }));
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("aiThreads").doc("thread_1").get()
  );
  await assertSucceeds(msgRef.delete());
  await assertSucceeds(threadRef.delete());
});

