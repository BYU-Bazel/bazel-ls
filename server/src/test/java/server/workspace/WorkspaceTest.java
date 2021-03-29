package server.workspace;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import server.bazel.cli.BazelServerException;
import server.bazel.tree.BuildTarget;
import server.bazel.tree.SourceFile;
import server.bazel.tree.WorkspaceTree;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkspaceTest {
    private Workspace classUnderTest;
    private List<BuildTarget> mockBuildTargetList;
    private List<SourceFile> mockSourceFileList;

    @Before
    public void setup() throws BazelServerException {
        classUnderTest = Mockito.spy(Workspace.getInstance());
        mockBuildTargetList = new ArrayList<>();
        mockSourceFileList = new ArrayList<>();

        Mockito.doReturn(mockBuildTargetList).when(classUnderTest).getBuildTargets();
        Mockito.doReturn(mockSourceFileList).when(classUnderTest).getSourceFiles();
    }

    @After
    public void tearDown() {
        classUnderTest.getWorkspaceTree().clearBelowPath("/");
        mockBuildTargetList = null;
        mockSourceFileList = null;
    }

    @Test
    public void testTreeWithSingleChildren() {
        mockSourceFileList.add(new SourceFile("TestFile1.java", Paths.get("main")));
        mockSourceFileList.add(new SourceFile("TestFile2.java", Paths.get("main/java")));
        mockSourceFileList.add(new SourceFile("TestFile3.java", Paths.get("main/java/test")));

        mockBuildTargetList.add(new BuildTarget(Paths.get("main"), "test_1", "test"));
        mockBuildTargetList.add(new BuildTarget(Paths.get("main/java"), "test_2", "test"));
        mockBuildTargetList.add(new BuildTarget(Paths.get("main/java/test"), "test_3", "test"));

        try{
            classUnderTest.syncWorkspace();
        } catch( BazelServerException e){
            System.out.println(e.getMessage());
        }
        
        WorkspaceTree tree = classUnderTest.getWorkspaceTree();
        WorkspaceTree.Node node = tree.getRoot();
        checkChildrenCount(node, 1);
    }

    @Test
    public void testTreeWithSeveralChildren() throws BazelServerException {
        mockSourceFileList.add(new SourceFile("TestFile1.java", Paths.get("main")));
        mockSourceFileList.add(new SourceFile("TestFile4.java", Paths.get("test")));
        mockSourceFileList.add(new SourceFile("TestFile2.java", Paths.get("main/java")));
        mockSourceFileList.add(new SourceFile("TestFile5.java", Paths.get("main/java1")));
        mockSourceFileList.add(new SourceFile("TestFile3.java", Paths.get("main/java/test")));
        mockSourceFileList.add(new SourceFile("TestFile6.java", Paths.get("main/java/test1")));

        mockBuildTargetList.add(new BuildTarget(Paths.get("main"), "test_1", "test"));
        mockBuildTargetList.add(new BuildTarget(Paths.get("test"), "test_4", "test"));
        mockBuildTargetList.add(new BuildTarget(Paths.get("main/java"), "test_2", "test"));
        mockBuildTargetList.add(new BuildTarget(Paths.get("main/java1"), "test_5", "test"));
        mockBuildTargetList.add(new BuildTarget(Paths.get("main/java/test"), "test_3", "test"));
        mockBuildTargetList.add(new BuildTarget(Paths.get("main/java/test1"), "test_6", "test"));

        classUnderTest.syncWorkspace();
        WorkspaceTree tree = classUnderTest.getWorkspaceTree();
        WorkspaceTree.Node node = tree.getRoot();
        checkChildrenCount(node, 2);
    }

    @Test
    public void testTreeWithSeveralBuildTargets() {
        mockBuildTargetList.add(new BuildTarget(Paths.get("main"), "test_1", "test"));
        mockBuildTargetList.add(new BuildTarget(Paths.get("main"), "test_2", "test"));
        mockBuildTargetList.add(new BuildTarget(Paths.get("main"), "test_3", "test"));
        try{
            classUnderTest.syncWorkspace();
        } catch( BazelServerException e){
            System.out.println(e.getMessage());
        }
        WorkspaceTree tree = classUnderTest.getWorkspaceTree();
        Optional<WorkspaceTree.Node> node = tree.getRoot().getChild("main");
        Assert.assertTrue(node.isPresent());
        Assert.assertEquals(3, node.get().getValue().getBuildTargets().size());
    }

    private void checkChildrenCount(WorkspaceTree.Node node, int expectedCount) {
        if(!node.isLeaf()) {
            Assert.assertEquals(expectedCount, node.getChildren().size());
            if(!node.isRoot()) {
                Assert.assertEquals(1, node.getValue().getBuildTargets().size());
                Assert.assertEquals(1, node.getValue().getSourceFiles().size());
            }
            for(WorkspaceTree.Node child : node.getChildren()) {
                checkChildrenCount(child, expectedCount);
            }
        }
    }
}
