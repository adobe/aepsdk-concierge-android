# Release notes

## Unreleased

### Theme configuration

- Added `behavior.citations.showCitationNumbers` (default: `true`) to hide inline
  citation number badges without hiding the Sources footer.
- Binary compatibility: adding this parameter to the public
  `ConciergeCitationsBehavior` data class changes its constructor and generated
  `copy` signatures. Kotlin source callers can continue using the default
  parameter, but consumers compiled against the previous SDK must be recompiled
  against the updated version.
