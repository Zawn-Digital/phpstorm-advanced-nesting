package com.zawndigital.advancednesting

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Automated tests for Advanced Nesting plugin.
 * These tests run headlessly in CI without requiring a full IDE instance.
 */
class AdvancedNestingTest : BasePlatformTestCase() {

    private lateinit var provider: AdvancedNestingTreeProvider

    override fun setUp() {
        super.setUp()
        provider = AdvancedNestingTreeProvider()
    }

    fun `test returns empty collection when children is empty`() {
        val result = provider.modify(mockParent, emptyList(), null)
        assertTrue(result.isEmpty())
    }

    fun `test returns original children when no PHP files present`() {
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(dirNode), settings)

        assertEquals(1, result.size)
    }

    fun `test returns original children when no directories present`() {
        val file = myFixture.addFileToProject("User.php", "<?php class User {}")
        val fileNode = PsiFileNode(project, file, settings)

        val result = provider.modify(mockParent, listOf(fileNode), settings)

        assertEquals(1, result.size)
    }

    fun `test creates nested group when matching pair exists`() {
        val file = myFixture.addFileToProject("User.php", "<?php class User {}")
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        assertTrue(result.first() is NestingGroupNode)
    }

    fun `test handles case-insensitive matching`() {
        val file = myFixture.addFileToProject("User.php", "<?php class User {}")
        val dir = myFixture.tempDirFixture.findOrCreateDir("user")  // lowercase
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        assertTrue(result.first() is NestingGroupNode)
    }

    fun `test handles multiple matching pairs`() {
        val userFile = myFixture.addFileToProject("User.php", "<?php class User {}")
        val roleFile = myFixture.addFileToProject("Role.php", "<?php class Role {}")

        val userDir = myFixture.tempDirFixture.findOrCreateDir("User")
        val roleDir = myFixture.tempDirFixture.findOrCreateDir("Role")

        val userPsiDir = psiManager.findDirectory(userDir)!!
        val rolePsiDir = psiManager.findDirectory(roleDir)!!

        val userFileNode = PsiFileNode(project, userFile, settings)
        val roleFileNode = PsiFileNode(project, roleFile, settings)
        val userDirNode = PsiDirectoryNode(project, userPsiDir, settings)
        val roleDirNode = PsiDirectoryNode(project, rolePsiDir, settings)

        val result = provider.modify(mockParent, listOf(userFileNode, userDirNode, roleFileNode, roleDirNode), settings)

        assertEquals(2, result.size)
        assertTrue(result.all { it is NestingGroupNode })
    }

    fun `test ignores non-PHP files`() {
        val jsFile = myFixture.addFileToProject("script.js", "console.log('test');")
        val jsFileNode = PsiFileNode(project, jsFile, settings)

        val result = provider.modify(mockParent, listOf(jsFileNode), settings)

        assertEquals(1, result.size)
        assertFalse(result.first() is NestingGroupNode)
    }

    fun `test directory without matching file remains visible`() {
        val file = myFixture.addFileToProject("User.php", "<?php class User {}")
        val userDir = myFixture.tempDirFixture.findOrCreateDir("User")
        val modelsDir = myFixture.tempDirFixture.findOrCreateDir("Models")

        val userPsiDir = psiManager.findDirectory(userDir)!!
        val modelsPsiDir = psiManager.findDirectory(modelsDir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val userDirNode = PsiDirectoryNode(project, userPsiDir, settings)
        val modelsDirNode = PsiDirectoryNode(project, modelsPsiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, userDirNode, modelsDirNode), settings)

        assertEquals(2, result.size)  // Grouped User.php + Models dir
        assertTrue(result.any { it is NestingGroupNode })
        assertTrue(result.contains(modelsDirNode))
    }

    fun `test nesting group node supports navigation`() {
        val file = myFixture.addFileToProject("User.php", "<?php class User {}")
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        val groupNode = result.first() as NestingGroupNode

        // Verify navigation capabilities
        assertTrue(groupNode.canNavigate())
        assertTrue(groupNode.canNavigateToSource())

        // Verify virtual file points to the PHP file, not the directory
        assertEquals(file.virtualFile, groupNode.virtualFile)

        // Verify expandOnDoubleClick is false (double-click opens file instead of expanding)
        assertFalse(groupNode.expandOnDoubleClick())
    }

    fun `test works with Ruby files`() {
        val file = myFixture.addFileToProject("User.rb", "class User; end")
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        assertTrue(result.first() is NestingGroupNode)
    }

    fun `test works with TypeScript files`() {
        val file = myFixture.addFileToProject("User.ts", "class User {}")
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        assertTrue(result.first() is NestingGroupNode)
    }

    fun `test works with Vue files`() {
        val file = myFixture.addFileToProject("User.vue", "<template><div>User</div></template>")
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        assertTrue(result.first() is NestingGroupNode)
    }

    fun `test works with Go files`() {
        val file = myFixture.addFileToProject("User.go", "package main")
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        assertTrue(result.first() is NestingGroupNode)
    }

    fun `test works with Python files`() {
        val file = myFixture.addFileToProject("User.py", "class User: pass")
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        assertTrue(result.first() is NestingGroupNode)
    }

    fun `test works with JSX files`() {
        val file = myFixture.addFileToProject("User.jsx", "export const User = () => <div>User</div>")
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        assertTrue(result.first() is NestingGroupNode)
    }

    fun `test works with TSX files`() {
        val file = myFixture.addFileToProject("User.tsx", "export const User = () => <div>User</div>")
        val dir = myFixture.tempDirFixture.findOrCreateDir("User")
        val psiDir = psiManager.findDirectory(dir)!!

        val fileNode = PsiFileNode(project, file, settings)
        val dirNode = PsiDirectoryNode(project, psiDir, settings)

        val result = provider.modify(mockParent, listOf(fileNode, dirNode), settings)

        assertEquals(1, result.size)
        assertTrue(result.first() is NestingGroupNode)
    }

    // Helpers

    private val mockParent: AbstractTreeNode<*>
        get() = object : AbstractTreeNode<String>(project, "root") {
            override fun getChildren() = emptyList<AbstractTreeNode<*>>()
            override fun update(p: com.intellij.ide.projectView.PresentationData) {}
        }

    private val settings: ViewSettings
        get() = object : ViewSettings {
            override fun isShowMembers() = false
            override fun isStructureView() = false
            override fun isShowModules() = false
            override fun isFlattenPackages() = false
            override fun isAbbreviatePackageNames() = false
            override fun isHideEmptyMiddlePackages() = false
            override fun isShowLibraryContents() = false
        }
}
