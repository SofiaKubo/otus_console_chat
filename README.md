# otus-console-chat

Project description will be added later.

## Code Style and Formatting

This project uses:

- `Spotless` for automatic Java formatting
- Eclipse JDT formatter for configurable Java line wrapping and spacing
- `Checkstyle` for style validation

### Check formatting and style

Run:

```bash
mvn verify -DskipTests
```

This will:

- check Java formatting with `spotless:check`
- validate style rules with `checkstyle:check`

### Auto-format the code

Run:

```bash
mvn spotless:apply
```

This will automatically format Java source files according to the project rules.

### Notes

- Formatting and style checks are configured in the root `pom.xml`
- Java formatter settings are stored in `config/formatter/eclipse-java-formatter.xml`
- Editor defaults are defined in `.editorconfig`
- Checkstyle rules are stored in `config/checkstyle/checkstyle.xml`
