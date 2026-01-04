# PhpStorm Plugin: Rails-Style File/Directory Nesting

## Problem Statement

PhpStorm's built-in file nesting feature only works for **files with the same name in the same directory**, not for file/directory combinations. We want to achieve Rails-style concerns organization:

```
app/Models/
├── User.php              (collapse/expand this)
│   └── User/             (nested under User.php)
│       ├── HasRoles.php
│       ├── HasPermissions.php
│       └── ...
```

Currently, PhpStorm cannot nest the `User/` directory under `User.php`.

## Research Summary

### PhpStorm Limitations
- File nesting only supports file-to-file relationships (e.g., `User.php`, `User.test.php`)
- No wildcards in nesting patterns
- No multilevel nesting
- Cannot nest directories under files
- [Official Documentation](https://www.jetbrains.com/help/phpstorm/file-nesting-dialog.html)

### Available Plugins
Investigated these plugins - **none support directory-under-file nesting**:
- [File Folder](https://plugins.jetbrains.com/plugin/29423-file-folder) - Groups files only
- [Groupper](https://plugins.jetbrains.com/plugin/10353-groupper) - Groups files by name only

### Solution
Build a custom PhpStorm plugin using the `TreeStructureProvider` extension point.

---

## Requirements

### Software
- **IntelliJ IDEA Community Edition** (free) - you develop PhpStorm plugins IN IntelliJ
- **Java 17+** or **Kotlin 2.x** (Kotlin recommended)
- **Gradle** (bundled with IntelliJ)
- **Plugin DevKit plugin** (install from JetBrains Marketplace)

### Knowledge
- Basic Java or Kotlin
- Understanding of tree data structures
- Familiarity with Gradle (minimal)

### Time Estimate
- **Proof of concept**: 4-6 hours
- **Production ready**: 15-20 hours
- **Published plugin**: +5 hours

---

## Architecture Overview

### How It Works

PhpStorm's project tree is built using a chain of `TreeStructureProvider` implementations. Each provider can:
1. Filter nodes (hide/show files)
2. Transform nodes (change presentation)
3. **Group/nest nodes** (our use case)

### Key Classes

| Class | Purpose |
|-------|---------|
| `TreeStructureProvider` | Interface for modifying project tree structure |
| `AbstractTreeNode<?>` | Base class for all tree nodes |
| `PsiFileNode` | Represents a file in the tree |
| `PsiDirectoryNode` | Represents a directory in the tree |
| `ViewSettings` | Settings for the current view |

### Extension Point

Register your implementation in `plugin.xml`:

```xml
<extensions defaultExtensionNs="com.intellij">
  <treeStructureProvider implementation="com.yourname.RailsStyleTreeProvider"/>
</extensions>
```

---

## Implementation Guide

### Step 1: Create Plugin Project

#### Option A: Use Template (Recommended)

1. Visit [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
2. Click "Use this template" → "Create a new repository"
3. Clone your new repository
4. Open in IntelliJ IDEA

#### Option B: New Project Wizard

1. Open IntelliJ IDEA
2. `File` → `New` → `Project`
3. Select **IDE Plugin**
4. Choose **Kotlin** (recommended) or Java
5. Set Platform: **PhpStorm 2025.1+**
6. Click **Create**

### Step 2: Configure Plugin Metadata

Edit `src/main/resources/META-INF/plugin.xml`:

```xml
<idea-plugin>
  <id>com.yourname.rails-style-nesting</id>
  <name>Rails-Style File Nesting</name>
  <vendor email="your@email.com" url="https://yoursite.com">Your Name</vendor>

  <description><![CDATA[
    Nest directories under PHP files with matching names (Rails-style concerns).

    Example: User/ directory will be nested under User.php in the project tree.
  ]]></description>

  <depends>com.intellij.modules.platform</depends>
  <depends>com.jetbrains.php</depends>

  <extensions defaultExtensionNs="com.intellij">
    <treeStructureProvider
      implementation="com.yourname.railsnesting.RailsStyleTreeProvider"
      order="last"/>
  </extensions>
</idea-plugin>
```

### Step 3: Implement TreeStructureProvider

Create `src/main/java/com/yourname/railsnesting/RailsStyleTreeProvider.java`:

```java
package com.yourname.railsnesting;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Nests directories under PHP files with matching names (Rails-style).
 *
 * Example: User.php + User/ directory -> User.php (with User/ nested inside)
 */
public class RailsStyleTreeProvider implements TreeStructureProvider {

  @NotNull
  @Override
  public Collection<AbstractTreeNode<?>> modify(
    @NotNull AbstractTreeNode<?> parent,
    @NotNull Collection<AbstractTreeNode<?>> children,
    ViewSettings settings
  ) {
    // Maps to track files and directories by base name
    Map<String, PsiFileNode> phpFiles = new HashMap<>();
    Map<String, PsiDirectoryNode> matchingDirs = new HashMap<>();
    List<AbstractTreeNode<?>> result = new ArrayList<>();

    // First pass: identify PHP files and directories
    for (AbstractTreeNode<?> child : children) {
      if (child instanceof PsiFileNode) {
        VirtualFile file = ((PsiFileNode) child).getVirtualFile();
        if (file != null && "php".equals(file.getExtension())) {
          String baseName = file.getNameWithoutExtension();
          phpFiles.put(baseName, (PsiFileNode) child);
        }
      } else if (child instanceof PsiDirectoryNode) {
        String dirName = ((PsiDirectoryNode) child).getValue().getName();
        matchingDirs.put(dirName, (PsiDirectoryNode) child);
      }
    }

    // Second pass: create grouped nodes for matching pairs
    Set<String> nestedDirs = new HashSet<>();

    for (AbstractTreeNode<?> child : children) {
      if (child instanceof PsiFileNode) {
        VirtualFile file = ((PsiFileNode) child).getVirtualFile();
        if (file != null && "php".equals(file.getExtension())) {
          String baseName = file.getNameWithoutExtension();

          // Check if there's a matching directory
          if (matchingDirs.containsKey(baseName)) {
            PsiDirectoryNode dirNode = matchingDirs.get(baseName);

            // Create custom node that wraps both file and directory
            result.add(new RailsStyleGroupNode(
              (PsiFileNode) child,
              dirNode,
              settings
            ));

            nestedDirs.add(baseName);
            continue;
          }
        }
      }

      // Skip directories that have been nested under files
      if (child instanceof PsiDirectoryNode) {
        String dirName = ((PsiDirectoryNode) child).getValue().getName();
        if (nestedDirs.contains(dirName)) {
          continue;
        }
      }

      // Add all other nodes as-is
      result.add(child);
    }

    return result;
  }
}
```

### Step 4: Create Custom Group Node

Create `src/main/java/com/yourname/railsnesting/RailsStyleGroupNode.java`:

```java
package com.yourname.railsnesting;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Custom tree node that displays a PHP file but contains a directory's children.
 *
 * This allows User.php to appear as a collapsible parent with User/ children.
 */
public class RailsStyleGroupNode extends AbstractTreeNode<PsiFile> {

  private final PsiFileNode fileNode;
  private final PsiDirectoryNode directoryNode;
  private final ViewSettings settings;

  public RailsStyleGroupNode(
    @NotNull PsiFileNode fileNode,
    @NotNull PsiDirectoryNode directoryNode,
    ViewSettings settings
  ) {
    super(fileNode.getProject(), fileNode.getValue());
    this.fileNode = fileNode;
    this.directoryNode = directoryNode;
    this.settings = settings;
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    // Return the directory's children (concerns)
    return directoryNode.getChildren();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    // Delegate presentation to the file node (shows User.php)
    fileNode.update(presentation);

    // Optional: Add indicator that this node has children
    // presentation.setLocationString("(concerns)");
  }
}
```

### Step 5: Build & Test

#### Run in Development Mode

```bash
./gradlew runIde
```

This launches a PhpStorm instance with your plugin loaded.

#### Test Cases
1. Create `app/Models/User.php` and `app/Models/User/` directory
2. Add some PHP files inside `User/`
3. Verify `User/` is nested under `User.php` in project tree
4. Test collapse/expand functionality
5. Test edge cases:
   - `User.php` exists but no `User/` directory
   - `User/` exists but no `User.php`
   - Both `User.php` and `user.php` (case sensitivity)

#### Package Plugin

```bash
./gradlew buildPlugin
```

Output: `build/distributions/plugin-name-version.zip`

---

## Advanced Features (Future Enhancements)

### 1. Settings UI

Allow users to configure:
- Which file extensions to match (`.php`, `.js`, etc.)
- Enable/disable the feature
- Custom naming patterns

### 2. Icon Differentiation

Add a custom icon or badge to grouped nodes:

```java
presentation.setIcon(AllIcons.Nodes.Folder); // Change icon
```

### 3. Context Menu Actions

Add "Ungoup" or "Create Concern" right-click menu options.

### 4. Performance Optimization

For large projects, cache matching pairs:

```java
private final Map<VirtualFile, PsiDirectoryNode> cache = new HashMap<>();
```

### 5. Multi-Level Support

Support nested concerns:
```
User.php
└── User/
    ├── HasRoles.php
    └── HasRoles/
        └── ...
```

---

## Resources

### Official Documentation
- [Quick Start Guide](https://plugins.jetbrains.com/docs/intellij/plugins-quick-start.html)
- [Creating Plugin Projects](https://plugins.jetbrains.com/docs/intellij/creating-plugin-project.html)
- [Modifying Project View Structure](https://plugins.jetbrains.com/docs/intellij/tree-structure-view.html)
- [Plugin DevKit](https://plugins.jetbrains.com/docs/intellij/developing-plugins.html)

### Example Code
- [TreeStructureProvider Sample](https://github.com/JetBrains/intellij-sdk-code-samples/tree/main/tree_structure_provider)
- [Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [IntelliJ Community Source](https://github.com/JetBrains/intellij-community)

### Tutorials
- [Writing IntelliJ Plugins - Baeldung](https://www.baeldung.com/intellij-new-custom-plugin)
- [Getting Started with Plugin Development](https://symflower.com/en/company/blog/2022/getting-started-with-intellij-plugin-development/)

### API Documentation
- [PsiDirectoryNode](https://dploeger.github.io/intellij-api-doc/com/intellij/ide/projectView/impl/nodes/PsiDirectoryNode.html)
- [AbstractTreeNode](https://www.programcreek.com/java-api-examples/?api=com.intellij.ide.util.treeView.AbstractTreeNode)

---

## Troubleshooting

### Plugin Not Loading
- Check `plugin.xml` syntax
- Verify `depends` tags include required modules
- Check IntelliJ logs: `Help` → `Show Log in Finder/Explorer`

### Tree Not Updating
- The `modify()` method may not be called on every change
- Implement `ProjectViewNode` refresh logic
- Check if other `TreeStructureProvider`s are conflicting

### Performance Issues
- Add caching for file/directory lookups
- Use `@NotNull` and `@Nullable` annotations properly
- Profile with IntelliJ's built-in profiler

### Custom Node Not Displaying Children
- Ensure `getChildren()` returns non-empty collection
- Check if `directoryNode.getChildren()` is being called
- Verify `ViewSettings` is passed correctly

---

## Publishing to JetBrains Marketplace

### Prerequisites
1. Create [JetBrains account](https://account.jetbrains.com/)
2. Join [JetBrains Marketplace](https://plugins.jetbrains.com/)
3. Prepare plugin metadata (description, screenshots, changelog)

### Steps
1. Build plugin: `./gradlew buildPlugin`
2. Test thoroughly in multiple PhpStorm versions
3. Upload to [Plugin Repository](https://plugins.jetbrains.com/plugin/add)
4. Fill in plugin details:
   - Name: "Rails-Style File Nesting"
   - Category: Navigation
   - Tags: php, laravel, rails, organization
5. Submit for review

### Marketing
- Write detailed description emphasizing Laravel/Rails developers
- Create demo GIF showing before/after
- Share on Reddit, Twitter, Laravel forums
- Reference Rails conventions in description

---

## Next Steps

When you're ready to implement:

1. **Install IntelliJ IDEA Community Edition**
   - Download: https://www.jetbrains.com/idea/download/
   - Install Plugin DevKit from `Settings` → `Plugins` → `Marketplace`

2. **Create project using template**
   - Use GitHub template or New Project wizard
   - Choose Kotlin if comfortable, Java otherwise

3. **Implement the three files**
   - `plugin.xml` (metadata)
   - `RailsStyleTreeProvider.java` (main logic)
   - `RailsStyleGroupNode.java` (custom node)

4. **Test thoroughly**
   - Run `./gradlew runIde`
   - Test with your `volanti-identity` project

5. **Share your work**
   - Publish to GitHub
   - Submit to JetBrains Marketplace
   - Help thousands of Rails/Laravel developers!

---

## Contact for Implementation Help

When you're back and ready to implement, ask Claude to:
1. Generate the complete project structure
2. Write the implementation files
3. Create test cases
4. Debug any issues
5. Optimize performance

This is a **valuable plugin** that doesn't currently exist - you could genuinely help the PHP/Laravel community!
