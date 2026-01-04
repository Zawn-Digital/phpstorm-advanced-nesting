package com.zawndigital.advancednesting

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiFile

/**
 * A custom tree node that wraps a file and shows directory children.
 *
 * Unlike a standard PsiFileNode, this node appears as a file but has children
 * from a matching directory. Double-clicking navigates to the file.
 */
class NestingGroupNode(
    private val fileNode: PsiFileNode,
    private val directoryNode: PsiDirectoryNode,
    settings: ViewSettings?
) : AbstractTreeNode<PsiFile>(fileNode.project, fileNode.value) {

    /**
     * Returns the children from the nested directory.
     */
    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        return directoryNode.children
    }

    /**
     * Display as the file.
     */
    override fun update(data: PresentationData) {
        fileNode.update(data)
    }

    /**
     * Navigate to the file when clicked.
     */
    override fun navigate(requestFocus: Boolean) {
        fileNode.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = fileNode.canNavigate()

    override fun canNavigateToSource(): Boolean = fileNode.canNavigateToSource()

    /**
     * This is key: return the file's virtual file so navigation works.
     */
    public override fun getVirtualFile() = fileNode.virtualFile

    /**
     * Tell the tree to prefer navigation over expansion on double-click.
     * This makes it behave like IntelliJ's built-in file nesting.
     */
    override fun expandOnDoubleClick(): Boolean = false
}
