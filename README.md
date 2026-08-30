# Freemarker Plus

An IntelliJ IDEA plugin that provides rich syntax highlighting for FreeMarker template files (`.ftl`), with embedded CSS and JavaScript support.

## Features

- **FreeMarker syntax highlighting** — comments, directives (`<#if>`, `<#list>`, `<#assign>`, ...), macro calls (`<@...>`), interpolation (`${...}`), strings, numbers, keywords, and operators.
- **HTML highlighting** — full HTML tag, attribute, and content highlighting via IntelliJ's built-in HTML lexer.
- **Embedded CSS highlighting** — CSS code inside `<style>` blocks is highlighted with the platform CSS lexer.
- **Embedded JavaScript highlighting** — JavaScript code inside `<script>` blocks is highlighted with the platform JS lexer.
- **Color scheme customization** — all FreeMarker token colors are configurable at **Settings → Editor → Color Scheme → Freemarker**.

## Supported File Types

| Extension | Description           |
|-----------|-----------------------|
| `.ftl`    | FreeMarker template   |

## Highlight Categories

The following categories are available in **Settings → Editor → Color Scheme → Freemarker**:

| Category                     | Default Color | Example                     |
|------------------------------|---------------|-----------------------------|
| Comment                      | Gray          | `<#-- comment -->`          |
| String                       | Green         | `"hello"`, `'world'`        |
| Keyword                      | Orange        | `gt`, `true`, `if`, `as`   |
| Directive name               | Purple        | `if`, `list`, `assign`      |
| Interpolation delimiters     | Gray          | `${`, `}`, `<#`, `>`        |
| Number                       | Blue          | `42`, `3.14`                |
| Operator                     | Gray          | `.`, `=`, `(`, `)`          |
| Bad character                | Red           | Unexpected tokens           |

## Installation

### From ZIP (Manual)

1. Download the `freemarker-plus-0.1.0.zip` from the releases.
2. In IntelliJ IDEA, go to **Settings → Plugins → ⚙ → Install Plugin from Disk...**
3. Select the downloaded ZIP file and restart the IDE.

### From Source

```bash
# Build the plugin ZIP
D:\gradle-dist\gradle-9.7.1\bin\gradle.bat buildPlugin

# The distributable is at:
# build/distributions/freemarker-plus-0.1.0.zip
```

To install from the built ZIP, follow the "From ZIP" instructions above.

## Development

### Prerequisites

- JDK 21
- Gradle 9.7.1 (bundled wrapper or local install)

### Build & Test

```bash
# Run all tests
D:\gradle-dist\gradle-9.7.1\bin\gradle.bat test

# Build the plugin distribution
D:\gradle-dist\gradle-9.7.1\bin\gradle.bat buildPlugin

# Launch a sandbox IDE with the plugin installed
D:\gradle-dist\gradle-9.7.1\bin\gradle.bat runIde
```

### Project Structure

```
freemarker-plus/
├── src/main/kotlin/com/freemarkerplus/
│   ├── lang/
│   │   ├── FreemarkerLanguage.kt       # Language definition (TemplateLanguage)
│   │   └── FreemarkerFileType.kt       # File type for .ftl
│   ├── lexer/
│   │   ├── FreemarkerTokenType.kt      # Token type constants
│   │   └── FreemarkerLexer.kt          # Hand-written lexer
│   └── highlighting/
│       ├── FreemarkerColors.kt          # TextAttributesKey definitions
│       ├── FreemarkerSyntaxHighlighter.kt       # LayeredLexer highlighter
│       ├── FreemarkerSyntaxHighlighterFactory.kt # Factory registration
│       └── FreemarkerColorSettingsPage.kt       # Color scheme settings page
├── src/main/resources/META-INF/
│   └── plugin.xml                       # Plugin descriptor
├── src/test/kotlin/com/freemarkerplus/
│   ├── lexer/
│   │   └── FreemarkerLexerTest.kt       # Lexer unit tests
│   └── highlighting/
│       └── FreemarkerSyntaxHighlighterTest.kt   # Highlighter integration tests
├── examples/
│   └── demo.ftl                         # Demo template covering all highlight scenarios
├── build.gradle.kts
└── settings.gradle.kts
```

## Demo

Open `examples/demo.ftl` in the sandbox IDE to see all highlighting features in action. The demo file covers:

- FreeMarker comments (`<#-- -->`)
- Directives (`<#if>`, `<#else>`, `</#if>`, `<#list>`, `</#list>`, `<#assign>`)
- Interpolation (`${user.name}`, `${title}`, `${item}`)
- Strings and numbers
- HTML structure (tags, attributes, doctype)
- Embedded CSS (inside `<style>`)
- Embedded JavaScript (inside `<script>`)

## Toolchain

| Component        | Version  |
|------------------|----------|
| IntelliJ Platform | 2026.2  |
| Kotlin           | 2.3.0   |
| IPGP             | 2.16.0  |
| Gradle           | 9.7.1   |
| JDK              | 21      |

## License

MIT
