package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorderStrong
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay

fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class BulletItem(val prefix: String, val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
}

@Composable
fun ChatMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        }
                        Text(
                            text = parseInlineMarkdown(block.text),
                            style = style.copy(fontWeight = FontWeight.Bold),
                            color = TextMain,
                        )
                    }

                    is MarkdownBlock.BulletItem -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = block.prefix,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = IndigoLight,
                            )
                            Text(
                                text = parseInlineMarkdown(block.text),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMain,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = parseInlineMarkdown(block.text),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMain,
                        )
                    }

                    is MarkdownBlock.CodeBlock -> {
                        CodeBlockCard(language = block.language, code = block.code)
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(language: String, code: String) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800L)
            copied = false
        }
    }

    Surface(
        color = ObsidianBg,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, StudioBorderStrong),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Surface(color = StudioSurfaceElevated) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = language.ifBlank { "code" }.lowercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                    )
                    IconButton(
                        onClick = {
                            copyTextToClipboard(context, "Code", code)
                            copied = true
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = if (copied) "Copied" else "Copy code",
                            tint = if (copied) EmeraldSuccess else TextMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Text(
                text = code,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                ),
                color = TextMain,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
    }
}

private fun parseMarkdownBlocks(input: String): List<MarkdownBlock> {
    val lines = input.replace("\r\n", "\n").lines()
    val result = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = StringBuilder()
    var inCodeBlock = false
    var codeLang = ""
    val codeBuffer = StringBuilder()

    fun flushParagraph() {
        val text = paragraphBuffer.toString().trim()
        if (text.isNotEmpty()) {
            result.add(MarkdownBlock.Paragraph(text))
        }
        paragraphBuffer.clear()
    }

    for (rawLine in lines) {
        val trimmed = rawLine.trim()
        if (trimmed.startsWith("```")) {
            if (!inCodeBlock) {
                flushParagraph()
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
                codeBuffer.clear()
            } else {
                result.add(MarkdownBlock.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
                inCodeBlock = false
                codeLang = ""
                codeBuffer.clear()
            }
            continue
        }

        if (inCodeBlock) {
            if (codeBuffer.isNotEmpty()) codeBuffer.append('\n')
            codeBuffer.append(rawLine)
            continue
        }

        when {
            trimmed.isEmpty() -> flushParagraph()
            trimmed.startsWith("### ") -> {
                flushParagraph()
                result.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("## ") -> {
                flushParagraph()
                result.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("# ") -> {
                flushParagraph()
                result.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                result.add(MarkdownBlock.BulletItem("•", trimmed.drop(2).trim()))
            }
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                flushParagraph()
                val dotIndex = trimmed.indexOf('.')
                val prefix = trimmed.substring(0, dotIndex + 1)
                val body = trimmed.substring(dotIndex + 1).trim()
                result.add(MarkdownBlock.BulletItem(prefix, body))
            }
            else -> {
                if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append('\n')
                paragraphBuffer.append(rawLine)
            }
        }
    }

    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        result.add(MarkdownBlock.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
    }
    flushParagraph()
    return result
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextMain)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                background = StudioSurfaceHigh,
                                color = CyanAccent,
                                fontSize = 13.sp,
                            )
                        ) {
                            append(" ${text.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '*' && !text.startsWith("**", i) -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
