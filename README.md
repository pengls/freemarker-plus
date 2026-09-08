# Freemarker Plus

An IntelliJ IDEA plugin that provides rich syntax highlighting and code navigation for FreeMarker template files (`.ftl`, `.ftlh`, `.ftlx`), with embedded CSS and JavaScript support.

## Features

- **FreeMarker syntax highlighting** — comments, directives (`<#if>`, `<#list>`, `<#assign>`, ...), macro calls (`<@...>`), interpolation (`${...}`), strings, numbers, keywords, and operators.
- **Expression support** — `?builtin` functions (with arguments, e.g. `${list?size}`, `${x?string("a","b")}`), `??` existence checks, parenthesized comparisons in directives (`<#if (a > b)>`), comparisons in interpolations, and block-level `<#assign x>...</#assign>`.
- **HTML highlighting** — full HTML tag, attribute, and content highlighting via IntelliJ's built-in HTML lexer.
- **Embedded CSS highlighting** — CSS code inside `<style>` blocks is highlighted with the platform CSS lexer.
- **Embedded JavaScript highlighting** — JavaScript code inside `<script>` blocks is highlighted with the platform JS lexer.
- **Color scheme customization** — all FreeMarker token colors are configurable at **Settings → Editor → Color Scheme → Freemarker**.

## Code Navigation (Phase 2)

- **Go to declaration** (Ctrl+B) — `#include`/`#import` file paths, `<@macro>` calls, `${variable}` references, `ns.member` namespaces.
- **Find usages** (Alt+F7) — macros, functions, variables.
- **Structure view** (Alt+7) — directives, macros, includes.
- **Code folding** — `<#if>/<#list>/<#macro>/<#function>/<#switch>` blocks.
- **Breadcrumbs** and **rename** (Shift+F6).

## HTML & JavaScript Navigation (Phase 2.5)

Since Phase 2.5, the HTML/CSS/JS data area of a `.ftl` file is parsed into real HTML PSI
(the platform template-language mechanism, same architecture as the official plugin):

- **`onclick="login()"` → JS function** — event-handler attributes resolve to the `login`
  function declared in the same file's `<script>` block, and to functions in external `.js`
  files referenced via `<script src="app.js">`.
- **`<script src="app.js">` → file** — Ctrl+B on the `src` path opens the JavaScript file.
- **HTML navigation** — tags and attributes get the platform's native navigation/usage support.

> Note: JavaScript features require the IDE's JavaScript support (bundled in IntelliJ IDEA
> Ultimate and WebStorm). On Community Edition without the JS plugin, HTML navigation still
> works; JS-specific navigation is simply unavailable.

## Supported File Types

| Extension | Description                      |
|-----------|----------------------------------|
| `.ftl`    | FreeMarker template (plain text) |
| `.ftlh`   | FreeMarker template (HTML)       |
| `.ftlx`   | FreeMarker template (XML)        |

## Highlight Categories

The following categories are available in **Settings → Editor → Color Scheme → Freemarker**:

| Category                     | Example                     |
|------------------------------|-----------------------------|
| Comment                      | `<#-- comment -->`          |
| String                       | `"hello"`, `'world'`        |
| Keyword                      | `gt`, `true`, `if`, `as`   |
| Directive name               | `if`, `list`, `assign`      |
| Interpolation delimiters     | `${`, `}`, `<#`, `>`        |
| Number                       | `42`, `3.14`                |
| Operator                     | `.`, `=`, `(`, `)`          |
| Bad character                | Unexpected tokens           |

Colors follow your active color scheme and are anchored to standard IntelliJ language defaults. Customize them under **Settings → Editor → Color Scheme → Freemarker**.

## Installation

### From JetBrains Marketplace

1. In IntelliJ IDEA, go to **Settings → Plugins → Marketplace**.
2. Search for **Freemarker Plus** and click **Install**.
3. Restart the IDE.

Plugin page: <https://plugins.jetbrains.com/plugin/33948-freemarker-plus>

### From ZIP (Manual)

1. Download the `freemarker-plus-0.4.0.zip` from the releases.
2. In IntelliJ IDEA, go to **Settings → Plugins → ⚙ → Install Plugin from Disk...**
3. Select the downloaded ZIP file and restart the IDE.

### From Source

```bash
# Build the plugin ZIP
./gradlew buildPlugin

# The distributable is at:
# build/distributions/freemarker-plus-0.4.0.zip
```

To install from the built ZIP, follow the "From ZIP" instructions above.

## Development

### Prerequisites

- JDK 25 (the IntelliJ 2026.2 bundled JBR, or any JDK 25; `jvmToolchain(25)` resolves it via `org.gradle.java.installations.paths`)
- Gradle 9.7.1 (bundled wrapper or local install)

### Build & Test

```bash
# Run all tests
./gradlew test

# Build the plugin distribution
./gradlew buildPlugin

# Launch a sandbox IDE with the plugin installed
./gradlew runIde
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

For HTML/JavaScript navigation, open `examples/js-navigation.ftl` (with `examples/app.js`):

- `Ctrl+B` on `login` inside `onclick="login()"` → the `function login` in the `<script>` block
- `Ctrl+B` on `logout` inside `onclick="logout()"` → `function logout` in `app.js`
- `Ctrl+B` on `app.js` inside `<script src="app.js">` → the external file

## Toolchain

| Component        | Version  |
|------------------|----------|
| IntelliJ Platform | 2026.2  |
| Kotlin           | 2.3.0   |
| IPGP             | 2.16.0  |
| Gradle           | 9.7.1   |
| JDK              | 25      |

## Publishing

To publish a new version to the JetBrains Marketplace:

```bash
# 1. Generate an upload token at plugins.jetbrains.com (avatar → Settings → Security →
#    Generate new token, with Upload permission), then set it as an environment variable:
$env:JETBRAINS_MARKETPLACE_TOKEN = "perm-..."

# 2. Build and upload (uploads build/distributions/<name>-<version>.zip):
./gradlew.bat publishPlugin
```

The upload lands in the default release channel and goes through JetBrains approval
before it becomes publicly visible.

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
