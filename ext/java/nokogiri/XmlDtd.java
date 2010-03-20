package nokogiri;

import nokogiri.internals.XmlDtdParser;
import org.cyberneko.dtd.DTDConfiguration;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

    /**
     * Create an unparented element that contains DTD declarations
     * parsed from <code>dtd</dtd>.  The owner document of each node
     * will be <code>doc</doc>.
     *
     * NekoDTD parser returns a new document node containing elements
     * representing the dtd declarations. The plan is to get the root
     * element and adopt it into <code>doc</code>, stipping the
     * Document provided by NekoDTD.
     */
    public static XmlDtd createXmlDtd(Ruby ruby, Document doc, String dtd) {
        Document dtdDoc = XmlDtdParser.parse(dtd);
        Node dtdNode = dtdDoc.getDocumentElement();
        doc.importNode(dtdNode, true); // adoptNode doesn't work here
                                       // (all attributes are empty
                                       // string; not sure why -Patrick)

        return new XmlDtd(ruby, dtdNode);
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
            String nodeName = child.getNodeName();
            IRubyObject decl = null;
            if (nodeName
                .equals(DTDConfiguration.E_ATTRIBUTE_DECL.localpart)) {
                decl = XmlAttributeDecl.create(context, child);
            } else if (nodeName
                       .equals(DTDConfiguration.E_ELEMENT_DECL.localpart)) {
                decl = XmlElementDecl.create(context, child);
            } else if (nodeName
                       .equals(DTDConfiguration.E_INTERNAL_ENTITY_DECL.localpart)) {
                decl = XmlEntityDecl.create(context, child);
            }

            if (decl != null)
                nodes.append(decl);

            extractDecls(context, child, nodes);
            child = child.getNextSibling();
        }
    }

}
