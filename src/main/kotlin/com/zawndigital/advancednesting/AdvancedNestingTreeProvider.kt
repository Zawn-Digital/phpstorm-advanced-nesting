package com.zawndigital.advancednesting

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile

/**
 * Modifies the project tree structure to nest directories under files with matching names.
 *
 * For example, if both `User.php` and `User/` directory exist as siblings,
 * the directory will be visually nested under the file in the project tree.
 *
 * Matching is case-insensitive: `User.php` will match `User/`, `user/`, or `USER/`.
 */
class AdvancedNestingTreeProvider : TreeStructureProvider {

    companion object {
        private const val PHP_EXTENSION = "php"
    }

    override fun modify(
        parent: AbstractTreeNode<*>,
        children: Collection<AbstractTreeNode<*>>,
        settings: ViewSettings?
    ): Collection<AbstractTreeNode<*>> {
        if (children.isEmpty()) return children

        val phpFiles = mutableMapOf<String, PsiFileNode>()
        val directories = mutableMapOf<String, PsiDirectoryNode>()

        // First pass: categorize children by type and name
        children.forEach { child ->
            when (child) {
                is PsiFileNode -> {
                    val file = child.virtualFile
                    if (file != null && file.extension?.lowercase() == PHP_EXTENSION) {
                        phpFiles[file.nameWithoutExtension.lowercase()] = child
                    }
                }
                is PsiDirectoryNode -> {
                    child.value?.name?.lowercase()?.let { dirName ->
                        directories[dirName] = child
                    }
                }
            }
        }

        // Early exit: if no PHP files or no directories, no nesting is possible
        if (phpFiles.isEmpty() || directories.isEmpty()) return children

        // Identify which directories have matching files
        val matchedDirectories = phpFiles.keys.intersect(directories.keys)

        // Early exit: if no matches, return original children unchanged
        if (matchedDirectories.isEmpty()) return children

        // Build result list: process all children, creating grouped nodes for matches
        val result = mutableListOf<AbstractTreeNode<*>>()

        children.forEach { child ->
            when (child) {
                is PsiFileNode -> {
                    val file = child.virtualFile
                    if (file != null && file.extension?.lowercase() == PHP_EXTENSION) {
                        val baseName = file.nameWithoutExtension.lowercase()
                        val matchingDir = directories[baseName]

                        if (matchingDir != null) {
                            // Create grouped node: file with directory's children
                            result.add(NestingGroupNode(child, matchingDir, settings))
                        } else {
                            // No matching directory, add file as-is
                            result.add(child)
                        }
                    } else {
                        // Non-PHP file, add as-is
                        result.add(child)
                    }
                }
                is PsiDirectoryNode -> {
                    val dirName = child.value?.name?.lowercase()
                    // Skip directories that have a matching PHP file (they're nested under the file)
                    if (dirName == null || !matchedDirectories.contains(dirName)) {
                        result.add(child)
                    }
                    // Matched directories are hidden (nested under their corresponding file)
                }
                else -> {
                    // Other node types, add as-is
                    result.add(child)
                }
            }
        }

        return result
    }
}
