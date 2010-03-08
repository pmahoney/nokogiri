package nokogiri;

import org.cyberneko.dtd.DTDConfiguration;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Node;

public class XmlDtd extends XmlNode {
    public static RubyClass getClass(Ruby ruby) {
        return (RubyClass)ruby.getClassFromPath("Nokogiri::XML::DTD");
    }

    public XmlDtd(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public XmlDtd(Ruby ruby, Node node) {
        this(ruby, getClass(ruby), node);
    }

    public XmlDtd(Ruby ruby, RubyClass rubyClass, Node node) {
        super(ruby, rubyClass, node);
    }

    @Override
    @JRubyMethod
    public IRubyObject attributes(ThreadContext context) {
        return context.getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject elements(ThreadContext context) {
        throw context.getRuntime().newNotImplementedError("not implemented");
    }

    @JRubyMethod
    public IRubyObject entities(ThreadContext context) {
        throw context.getRuntime().newNotImplementedError("not implemented");
    }

    @JRubyMethod
    public IRubyObject notations(ThreadContext context) {
        throw context.getRuntime().newNotImplementedError("not implemented");
    }

    /**
     * Our "node" object is as-returned by NekoDTD.  The actual
     * "children" that we're interested in (Attribute declarations,
     * etc.) are a few layers deep.
     */
    @Override
    @JRubyMethod
    public IRubyObject children(ThreadContext context) {
        RubyArray children = RubyArray.newArray(context.getRuntime());
        extractDecls(context, getNode(), children);
        return new XmlNodeSet(context.getRuntime(),
                              (RubyClass)context.getRuntime()
                              .getClassFromPath("Nokogiri::XML::NodeSet"),
                              children);
    }

    protected void extractDecls(ThreadContext context, Node node,
                                RubyArray nodes) {
        Node child = node.getFirstChild();
        while (child != null) {
            if (child.getNodeName()
                .equals(DTDConfiguration.E_ATTRIBUTE_DECL.localpart)) {
                IRubyObject attrDecl =
                    XmlAttributeDecl.create(context, child);
                nodes.append(attrDecl);
            }

            extractDecls(context, child, nodes);
            child = child.getNextSibling();
        }
    }

}
