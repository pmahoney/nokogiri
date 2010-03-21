package nokogiri;

import nokogiri.internals.XmlDtdParser;
import org.apache.xerces.xni.QName;
import org.cyberneko.dtd.DTDConfiguration;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlDtd extends XmlNode {
    protected RubyArray allDecls = null;

    /** cache of children, Nokogiri::XML::NodeSet */
    protected IRubyObject children = null;

    /** cache of name => XmlAttributeDecl */
    protected RubyHash attributes = null;

    /** cache of name => XmlElementDecl */
    protected RubyHash elements = null;

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
        if (attributes == null) extractDecls(context);

        return attributes;
    }

    @JRubyMethod
    public IRubyObject elements(ThreadContext context) {
        if (elements == null) extractDecls(context);

        return elements;
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
        if (children == null) extractDecls(context);

        return children;
    }

    protected static boolean nameEquals(Node node, QName name) {
        return name.localpart.equals(node.getNodeName());
    }

    public static boolean isAttributeDecl(Node node) {
        return nameEquals(node, DTDConfiguration.E_ATTRIBUTE_DECL);
    }

    public static boolean isElementDecl(Node node) {
        return nameEquals(node, DTDConfiguration.E_ELEMENT_DECL);
    }

    public static boolean isEntityDecl(Node node) {
        return nameEquals(node, DTDConfiguration.E_INTERNAL_ENTITY_DECL);
    }

    /**
     * Recursively extract various DTD declarations and store them in
     * the various collections.
     */
    protected void extractDecls(ThreadContext context) {
        Ruby runtime = context.getRuntime();

        // initialize data structures
        allDecls = RubyArray.newArray(runtime);
        attributes = RubyHash.newHash(runtime);
        elements = RubyHash.newHash(runtime);

        // recursively extract decls
        extractDecls(context, getNode());

        // convert allDecls to a NodeSet
        children =
            new XmlNodeSet(context.getRuntime(),
                           (RubyClass)
                           runtime.getClassFromPath("Nokogiri::XML::NodeSet"),
                           allDecls);

        // add attribute decls as attributes to the matching element decl
        RubyArray keys = attributes.keys();
        for (int i = 0; i < keys.getLength(); ++i) {
            IRubyObject akey = keys.entry(i);
            IRubyObject val;

            val = attributes.op_aref(context, akey);
            if (val.isNil()) continue;
            XmlAttributeDecl attrDecl = (XmlAttributeDecl) val;
            IRubyObject ekey = attrDecl.element_name(context);
            val = elements.op_aref(context, ekey);
            if (val.isNil()) continue;
            XmlElementDecl elemDecl = (XmlElementDecl) val;

            elemDecl.appendAttrDecl(attrDecl);
        }
    }

    protected void extractDecls(ThreadContext context, Node node) {
        Node child = node.getFirstChild();
        while (child != null) {
            if (isAttributeDecl(child)) {
                XmlAttributeDecl decl = (XmlAttributeDecl)
                    XmlAttributeDecl.create(context, child);
                attributes.op_aset(context, decl.attribute_name(context), decl);
                allDecls.append(decl);
            } else if (isElementDecl(child)) {
                XmlElementDecl decl = (XmlElementDecl)
                    XmlElementDecl.create(context, child);
                elements.op_aset(context, decl.element_name(context), decl);
                allDecls.append(decl);
            } else if (isEntityDecl(child)) {
                IRubyObject decl = XmlEntityDecl.create(context, child);
                allDecls.append(decl);
            }

            extractDecls(context, child);
            child = child.getNextSibling();
        }
    }

}
