package com.zawndigital.advancednesting

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiDirectory

/**
 * A custom tree node that displays as a file but contains a directory's children.
 *
 * This enables the visual nesting pattern where `User.php` appears in the tree
 * with an expansion arrow, and expanding it reveals the contents of the `User/` directory.
 *
 * @property fileNode The PHP file node (e.g., User.php) used for display
 * @property directoryNode The matching directory node (e.g., User/) providing children
 * @property settings View settings from the current project view
 */
class NestingGroupNode(
    private val fileNode: PsiFileNode,
    private val directoryNode: PsiDirectoryNode,
    private val settings: ViewSettings?
) : AbstractTreeNode<PsiDirectory>(fileNode.project, directoryNode.value) {

    /**
     * Returns the children from the nested directory.
     *
     * This is what makes the grouping work: when the user expands this node,
     * they see the directory's contents, not the file's children.
     */
    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        return directoryNode.children
    }

    /**
     * Updates the presentation to display as the file.
     *
     * Delegates to the file node so this appears exactly like a regular PHP file
     * in the tree, but with children (expansion arrow) when a matching directory exists.
     */
    override fun update(presentation: PresentationData) {
        fileNode.update(presentation)
    }

    /**
     * Enables navigation to the file when clicked.
     */
    override fun canNavigate(): Boolean = fileNode.canNavigate()

    /**
     * Enables "Navigate to Source" action.
     */
    override fun canNavigateToSource(): Boolean = fileNode.canNavigateToSource()

    /**
     * Navigates to the file (not the directory).
     */
    override fun navigate(requestFocus: Boolean) {
        fileNode.navigate(requestFocus)
    }
}
