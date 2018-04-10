package java10;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class DynamicProxyTests {
    public static interface IMyriadLink {
        String getLinkType();
        String getLeftNode();
        String getRightNode();
    }

    public static class MyriadLink implements IMyriadLink {
        private String linkType;
        private String leftNode;
        private String rightNode;

        public MyriadLink() {
        }

        public MyriadLink(String linkType, String leftNode, String rightNode) {
            this.linkType = linkType;
            this.leftNode = leftNode;
            this.rightNode = rightNode;
        }

        public String getLinkType() {
            return linkType;
        }

        public void setLinkType(String linkType) {
            this.linkType = linkType;
        }

        public String getLeftNode() {
            return leftNode;
        }

        public void setLeftNode(String leftNode) {
            this.leftNode = leftNode;
        }

        public String getRightNode() {
            return rightNode;
        }

        public void setRightNode(String rightNode) {
            this.rightNode = rightNode;
        }
    }

    public static interface IMyriadNode {
        String getId();
        List<IMyriadLink> getNl();
    }

    public static class MyriadNode implements IMyriadNode {
        private String id;
        private List<IMyriadLink> nl;

        public MyriadNode() {
        }

        public MyriadNode(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<IMyriadLink> getNl() {
            return nl;
        }

        public void setNl(List<IMyriadLink> nl) {
            this.nl = nl;
        }
    }

    public static class MyriadAccessor {
        public IMyriadNode getNode(String id) {
            IMyriadNode node = new MyriadNode(id);

            IMyriadNode proxiedNode = (IMyriadNode) Proxy.newProxyInstance(
                    DynamicProxyTests.class.getClassLoader(),
                    new Class[]{ IMyriadNode.class },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getId":
                                System.out.printf("this is %s\n", this.getClass().getName());
                                return node.getId();
                            case "getNl":
                                return node.getNl();
                        }
                        return null;
                    });

            return proxiedNode;
        }
    }

    public void test1() {
        MyriadAccessor ma = new MyriadAccessor();
        IMyriadNode node1 = ma.getNode("1");
        System.out.println(node1.getId());
    }
}
