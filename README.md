![icon.png](https://cdn.modrinth.com/data/I70N6bTC/a38a4215bb7e541cd4d4d7aaea7883effd18b9e1.png)

**Simple Modpack Update Checker** is a lightweight mod designed to check if your modpack is up-to-date. This mod
verifies the version of your modpack against a remote source and notifies you if an update is available.

## Supported Versions

|           | [smuc 1.0.0](https://modrinth.com/mod/smuc/version/1.0.0) | [smuc 2.0.0](https://modrinth.com/mod/smuc/version/2.0.0) |
|-----------|:---------------------------------------------------------:|:---------------------------------------------------------:|
| mc 1.21   |                             ✅                             |                             ✅                             |
| mc 1.21.1 |                             ✅                             |                             ✅                             |
| mc 1.21.2 |                             ✅                             |                             ✅                             |
| mc 1.21.3 |                             ✅                             |                             ✅                             |
| mc 1.21.4 |                             ✅                             |                             ✅                             |
| mc 1.21.5 |                             ✅                             |                             ✅                             |
| mc 1.21.6 |                             ✅                             |                             ✅                             |
| mc 1.21.7 |                             ✅                             |                             ✅                             |
| mc 1.21.8 |                             ✅                             |                             ✅                             |

## Configuration

The configuration file is located at `config/simple-modpack-update-checker.json`. For most users, only three fields are required:

```json
{
  "configVersion": 2,
  "localVersion": "3.3.3",
  "identifier": "KmiWHzQ4"
}
```

1. **configVersion**: Configuration file version (automatically set)
2. **localVersion**: The current version of the installed modpack. This can be any string.
3. **identifier**: Either a URL to a text file containing the latest version (formatted as `version = "String"`, eg [your pack.toml from packwiz](https://raw.githubusercontent.com/SkyblockerMod/Skyblocker-modpack/main/packwiz/pack.toml)) or the Modrinth project ID.

### Configuration Options

| Field | Required | Description                           | Default      | Example                |
|-------|----------|---------------------------------------|--------------|------------------------|
| `configVersion` | ✅ | Configuration file version            | `2`          | `2`                    |
| `localVersion` | ✅ | Current version of your modpack       | -            | `"3.3.3"`              |
| `identifier` | ✅ | Modrinth project ID or URL            | -            | `"KmiWHzQ4"`           |
| `minecraftVersions` | ❌ | Specific Minecraft <br/>version to track <br/>**(Modrinth only)** | All versions | `["1.21.4", "1.21.5"]` |
| `releaseChannel` | ❌ | Release channel to follow <br/>**(Modrinth only)**            | `"release"`  | `"beta"`, `"alpha"`    |

**Note:** `minecraftVersions` and `releaseChannel` only work when using a Modrinth project ID. When using a URL, these options are ignored and the mod will simply check the version string from your URL.

### Release Channels

**Note:** Release channels only work when using a **Modrinth project ID**. URL-based configurations will ignore this setting.

The mod supports different release channels that work in a **hierarchical** way - more unstable channels include all the more stable ones:

#### Channel Types
- **`"release"` (default)**: Only stable releases
- **`"beta"`**: Beta and stable releases
- **`"alpha"`**: Alpha, beta, and stable releases (everything)

#### Example Scenario
If a project has these versions (newest first):
1. `2.1.0-alpha` (yesterday)
2. `2.0.5` (release, 3 days ago)
3. `2.0.4-beta` (1 week ago)

**Results by channel:**
- `"alpha"` → Shows `2.1.0-alpha` (latest of any type)
- `"beta"` → Shows `2.0.5` (latest beta/release, skips alpha)
- `"release"` → Shows `2.0.5` (latest release only)

### Examples

**Using a URL (basic version checking only)**:

```json
{
  "configVersion": 2,
  "localVersion": "3.3.3",
  "identifier": "https://raw.githubusercontent.com/SkyblockerMod/Skyblocker-modpack/main/packwiz/pack.toml"
}
```

**Using a Modrinth Project ID (basic)**:

```json
{
  "configVersion": 2,
  "localVersion": "3.3.3",
  "identifier": "KmiWHzQ4"
}
```

**With Minecraft version filtering (Modrinth only)**:

```json
{
  "configVersion": 2,
  "localVersion": "3.3.3",
  "identifier": "KmiWHzQ4",
  "minecraftVersions": ["1.21.4"]
}
```

**With beta release channel (Modrinth only)**:

```json
{
  "configVersion": 2,
  "localVersion": "3.3.3-beta.5",
  "identifier": "KmiWHzQ4",
  "releaseChannel": "beta"
}
```

**With multiple Minecraft versions and release channel (Modrinth only)**:

```json
{
  "configVersion": 2,
  "localVersion": "3.3.3",
  "identifier": "KmiWHzQ4",
  "minecraftVersions": ["1.21.4", "1.21.5"],
  "releaseChannel": "alpha"
}
```

## Usage

When you start Minecraft with this mod installed, it will automatically check for updates based on your configuration
file and notify you if an update is available.

### For Modrinth Projects
- The mod fetches version information from the Modrinth API
- Supports filtering by Minecraft version(s) and release channels
- Shows notifications for newer versions based on your configured filters

### For URL-Based Checks
- The mod fetches content from your specified URL
- Looks for version information in the format `version = "String"`
- Compatible with packwiz pack.toml files
- **Note:** Minecraft version filtering and release channels are not supported with URLs

![img.png](https://cdn.modrinth.com/data/I70N6bTC/images/1babc5d3fef1b29f8fd1aa49f845edc21b1d7774.png)

