# Freemarker Plus

An IntelliJ IDEA plugin that provides rich syntax highlighting and code navigation for FreeMarker template files (`.ftl`, `.ftlh`, `.ftlx`), with embedded CSS and JavaScript support.

## Features

- **FreeMarker syntax highlighting** 鈥?comments, directives (`<#if>`, `<#list>`, `<#assign>`, ...), macro calls (`<@...>`), interpolation (`${...}`), strings, numbers, keywords, and operators.
- **HTML highlighting** 鈥?full HTML tag, attribute, and content highlighting via IntelliJ's built-in HTML lexer.
- **Embedded CSS highlighting** 鈥?CSS code inside `<style>` blocks is highlighted with the platform CSS lexer.
- **Embedded JavaScript highlighting** 鈥?JavaScript code inside `<script>` blocks is highlighted with the platform JS lexer.
- **Color scheme customization** 鈥?all FreeMarker token colors are configurable at **Settings 鈫?Editor 鈫?Color Scheme 鈫?Freemarker**.

## Code Navigation (Phase 2)

- **Go to declaration** (Ctrl+B) 鈥?`#include`/`#import` file paths, `<@macro>` calls, `${variable}` references, `ns.member` namespaces.
- **Find usages** (Alt+F7) 鈥?macros, functions, variables.
- **Structure view** (Alt+7) 鈥?directives, macros, includes.
- **Code folding** 鈥?`<#if>/<#list>/<#macro>/<#function>/<#switch>` blocks.
- **Breadcrumbs** and **rename** (Shift+F6).

## HTML & JavaScript Navigation (Phase 2.5)

Since Phase 2.5, the HTML/CSS/JS data area of a `.ftl` file is parsed into real HTML PSI
(the platform template-language mechanism, same architecture as the official plugin):

- **`onclick="login()"` 鈫?JS function** 鈥?event-handler attributes resolve to the `login`
  function declared in the same file's `<script>` block, and to functions in external `.js`
  files referenced via `<script src="app.js">`.
- **`<script src="app.js">` 鈫?file** 鈥?Ctrl+B on the `src` path opens the JavaScript file.
- **HTML navigation** 鈥?tags and attributes get the platform's native navigation/usage support.

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

The following categories are available in **Settings 鈫?Editor 鈫?Color Scheme 鈫?Freemarker**:

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

Colors follow your active color scheme and are anchored to standard IntelliJ language defaults. Customize them under **Settings 鈫?Editor 鈫?Color Scheme 鈫?Freemarker**.

## Installation

### From ZIP (Manual)

1. Download the `freemarker-plus-0.3.1.zip` from the releases.
2. In IntelliJ IDEA, go to **Settings 鈫?Plugins 鈫?鈿?鈫?Install Plugin from Disk...**
3. Select the downloaded ZIP file and restart the IDE.

### From Source

```bash
# Build the plugin ZIP
./gradlew buildPlugin

# The distributable is at:
# build/distributions/freemarker-plus-0.3.1.zip
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
鈹溾攢鈹€ src/main/kotlin/com/freemarkerplus/
鈹?  鈹溾攢鈹€ lang/
鈹?  鈹?  鈹溾攢鈹€ FreemarkerLanguage.kt       # Language definition (TemplateLanguage)
鈹?  鈹?  鈹斺攢鈹€ FreemarkerFileType.kt       # File type for .ftl
鈹?  鈹溾攢鈹€ lexer/
鈹?  鈹?  鈹溾攢鈹€ FreemarkerTokenType.kt      # Token type constants
鈹?  鈹?  鈹斺攢鈹€ FreemarkerLexer.kt          # Hand-written lexer
鈹?  鈹斺攢鈹€ highlighting/
鈹?      鈹溾攢鈹€ FreemarkerColors.kt          # TextAttributesKey definitions
鈹?      鈹溾攢鈹€ FreemarkerSyntaxHighlighter.kt       # LayeredLexer highlighter
鈹?      鈹溾攢鈹€ FreemarkerSyntaxHighlighterFactory.kt # Factory registration
鈹?      鈹斺攢鈹€ FreemarkerColorSettingsPage.kt       # Color scheme settings page
鈹溾攢鈹€ src/main/resources/META-INF/
鈹?  鈹斺攢鈹€ plugin.xml                       # Plugin descriptor
鈹溾攢鈹€ src/test/kotlin/com/freemarkerplus/
鈹?  鈹溾攢鈹€ lexer/
鈹?  鈹?  鈹斺攢鈹€ FreemarkerLexerTest.kt       # Lexer unit tests
鈹?  鈹斺攢鈹€ highlighting/
鈹?      鈹斺攢鈹€ FreemarkerSyntaxHighlighterTest.kt   # Highlighter integration tests
鈹溾攢鈹€ examples/
鈹?  鈹斺攢鈹€ demo.ftl                         # Demo template covering all highlight scenarios
鈹溾攢鈹€ build.gradle.kts
鈹斺攢鈹€ settings.gradle.kts
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

- `Ctrl+B` on `login` inside `onclick="login()"` 鈫?the `function login` in the `<script>` block
- `Ctrl+B` on `logout` inside `onclick="logout()"` 鈫?`function logout` in `app.js`
- `Ctrl+B` on `app.js` inside `<script src="app.js">` 鈫?the external file

## Toolchain

| Component        | Version  |
|------------------|----------|
| IntelliJ Platform | 2026.2  |
| Kotlin           | 2.3.0   |
| IPGP             | 2.16.0  |
| Gradle           | 9.7.1   |
| JDK              | 25      |

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
