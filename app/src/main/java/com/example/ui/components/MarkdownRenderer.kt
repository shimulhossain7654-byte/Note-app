package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    markdownText: String,
    modifier: Modifier = Modifier
) {
    val lines = markdownText.split("\n")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var inCodeBlock = false
        val currentCodeLines = mutableListOf<String>()

        for (line in lines) {
            val trimmedLine = line.trim()

            // Handle block code fences
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    // Render accumulated code lines
                    CodeBlock(currentCodeLines.joinToString("\n"))
                    currentCodeLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                currentCodeLines.add(line)
                continue
            }

            // Headers
            if (trimmedLine.startsWith("# ")) {
                Text(
                    text = parseInlineStyles(trimmedLine.removePrefix("# ")),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            } else if (trimmedLine.startsWith("## ")) {
                Text(
                    text = parseInlineStyles(trimmedLine.removePrefix("## ")),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            } else if (trimmedLine.startsWith("### ")) {
                Text(
                    text = parseInlineStyles(trimmedLine.removePrefix("### ")),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            // Blockquotes
            else if (trimmedLine.startsWith(">")) {
                val quoteContent = trimmedLine.removePrefix(">").trim()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(IntrinsicSize.Min)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            .align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = parseInlineStyles(quoteContent),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            // Checklists
            else if (trimmedLine.startsWith("- [ ]") || trimmedLine.startsWith("- [x]") || trimmedLine.startsWith("- [X]")) {
                val checked = trimmedLine.startsWith("- [x]") || trimmedLine.startsWith("- [X]")
                val checkContent = if (checked) {
                    trimmedLine.substring(5).trim()
                } else {
                    trimmedLine.substring(5).trim()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (checked) "Checked" else "Unchecked",
                        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = parseInlineStyles(checkContent),
                        fontSize = 15.sp,
                        color = if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // Unordered/Bullet lists
            else if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")) {
                val listContent = trimmedLine.substring(2).trim()
                Row(
                    modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = parseInlineStyles(listContent),
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // Numbered lists
            else if (trimmedLine.isNotEmpty() && trimmedLine.first().isDigit() && trimmedLine.contains(". ")) {
                val dotIndex = trimmedLine.indexOf(". ")
                if (dotIndex > 0 && trimmedLine.substring(0, dotIndex).all { it.isDigit() }) {
                    val number = trimmedLine.substring(0, dotIndex)
                    val contentNum = trimmedLine.substring(dotIndex + 2).trim()
                    Row(
                        modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$number.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineStyles(contentNum),
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // Standard text block
                    StandardLine(line)
                }
            }
            // Divider / Hard rule
            else if (trimmedLine == "---" || trimmedLine == "***") {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
            }
            // Standard text lines
            else {
                if (line.isNotEmpty()) {
                    StandardLine(line)
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
        // If code block is not closed
        if (inCodeBlock && currentCodeLines.isNotEmpty()) {
            CodeBlock(currentCodeLines.joinToString("\n"))
        }
    }
}

@Composable
fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun StandardLine(line: String) {
    Text(
        text = parseInlineStyles(line),
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}

// Parses **bold** and *italic* and `inline code` within a line of markdown text
private fun parseInlineStyles(text: String) = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        val boldStart = text.indexOf("**", index)
        val italicStart = text.indexOf("*", index)
        val codeStart = text.indexOf("`", index)

        val nextIndex = listOf(
            if (boldStart != -1) boldStart else Int.MAX_VALUE,
            if (italicStart != -1) italicStart else Int.MAX_VALUE,
            if (codeStart != -1) codeStart else Int.MAX_VALUE
        ).minOrNull() ?: Int.MAX_VALUE

        if (nextIndex == Int.MAX_VALUE) {
            append(text.substring(index))
            break
        }

        if (nextIndex > index) {
            append(text.substring(index, nextIndex))
        }

        when (nextIndex) {
            boldStart -> {
                val boldEnd = text.indexOf("**", boldStart + 2)
                if (boldEnd != -1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(boldStart + 2, boldEnd))
                    }
                    index = boldEnd + 2
                } else {
                    append("**")
                    index = boldStart + 2
                }
            }
            codeStart -> {
                val codeEnd = text.indexOf("`", codeStart + 1)
                if (codeEnd != -1) {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Black.copy(alpha = 0.1f),
                            fontSize = 14.sp
                        )
                    ) {
                        append(text.substring(codeStart + 1, codeEnd))
                    }
                    index = codeEnd + 1
                } else {
                    append("`")
                    index = codeStart + 1
                }
            }
            italicStart -> {
                val italicEnd = text.indexOf("*", italicStart + 1)
                if (italicEnd != -1) {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(italicStart + 1, italicEnd))
                    }
                    index = italicEnd + 1
                } else {
                    append("*")
                    index = italicStart + 1
                }
            }
            else -> {
                index = nextIndex + 1
            }
        }
    }
}
