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
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static nokogiri.internals.NokogiriHelpers.stringOrNil;
import static org.jruby.javasupport.util.RuntimeHelpers.invoke;

public class XmlDtd extends XmlNode {
    /**
     * The as-parsed by NekoDTD and worked over by
     * newFromInternalSubset() node containnit DTD decls.
     */
    protected Node dtdNode;

    protected RubyArray allDecls = null;

    /** cache of children, Nokogiri::XML::NodeSet */
    protected IRubyObject children = null;

    /** cache of name => XmlAttributeDecl */
    protected RubyHash attributes = null;

    /** cache of name => XmlElementDecl */
    protected RubyHash elements = null;

    /** cache of name => XmlEntityDecl */
    protected RubyHash entities = null;

    /** cache of name => Nokogiri::XML::Notation */
    protected RubyHash notations = null;
    protected RubyClass notationClass;

    /** temporary store of content models before they are added to
     * their XmlElementDecl. */
    protected RubyHash contentModels;

    public static RubyClass getClass(Ruby ruby) {
        return (RubyClass)ruby.getClassFromPath("Nokogiri::XML::DTD");
    }

    public XmlDtd(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public XmlDtd(Ruby ruby, DocumentType node, Node dtdNode) {
        this(ruby, getClass(ruby), node, dtdNode);
    }

    public XmlDtd(Ruby ruby, RubyClass rubyClass,
                  DocumentType node, Node dtdNode) {
        super(ruby, rubyClass, node);
        this.dtdNode = dtdNode;
        notationClass = (RubyClass)
            ruby.getClassFromPath("Nokogiri::XML::Notation");
    }

    /**
     * Create an unparented element that contains DTD declarations
     * parsed from the internal subset of <code>doctype</dtd>.  The
     * owner document of each node will be the owner of
     * <code>doctype</doc>.
     *
     * NekoDTD parser returns a new document node containing elements
     * representing the dtd declarations. The plan is to get the root
     * element and adopt it into the correct document, stipping the
     * Document provided by NekoDTD.
     */
    public static XmlDtd newFromInternalSubset(Ruby ruby, DocumentType node) {
        Document doc = node.getOwnerDocument();
        String dtd = node.getInternalSubset();
        if (dtd == null) {
            return new XmlDtd(ruby, node, null);
        }

        Document dtdDoc = XmlDtdParser.parse(dtd);
        Node dtdNode = dtdDoc.getDocumentElement();
        doc.importNode(dtdNode, true); // adoptNode doesn't work here
                                       // (all attributes are empty
                                       // string; not sure why -Patrick)

        return new XmlDtd(ruby, node, dtdNode);
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
        if (entities == null) extractDecls(context);

        return entities;
    }

    @JRubyMethod
    public IRubyObject notations(ThreadContext context) {
        if (notations == null) extractDecls(context);

        return notations;
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

    /**
     * Returns the name of the dtd.
     */
    @Override
    @JRubyMethod
    public IRubyObject node_name(ThreadContext context) {
        DocumentType dt = (DocumentType) getNode();
        return context.getRuntime().newString(dt.getName());
    }

    @Override
    @JRubyMethod(name = "node_name=")
    public IRubyObject node_name_set(ThreadContext context, IRubyObject name) {
        throw context.getRuntime()
            .newRuntimeError("cannot change name of DTD");
    }

    /**
     * Return the basename of the system id.  E.g. for system id of
     * "files/staff.xml", return "staff.xml".
     */
    @JRubyMethod
    public IRubyObject system_id(ThreadContext context) {
        DocumentType dt = (DocumentType) getNode();
        return context.getRuntime().newString(dt.getSystemId());
    }

    @JRubyMethod
    public IRubyObject external_id(ThreadContext context) {
        DocumentType dt = (DocumentType) getNode();
        return stringOrNil(context.getRuntime(), dt.getPublicId());
    }

    public static boolean nameEquals(Node node, QName name) {
        return name.localpart.equals(node.getNodeName());
    }

    public static boolean isAttributeDecl(Node node) {
        return nameEquals(node, DTDConfiguration.E_ATTRIBUTE_DECL);
    }

    public static boolean isElementDecl(Node node) {
        return nameEquals(node, DTDConfiguration.E_ELEMENT_DECL);
    }

    public static boolean isEntityDecl(Node node) {
        return (nameEquals(node, DTDConfiguration.E_INTERNAL_ENTITY_DECL) ||
                nameEquals(node, DTDConfiguration.E_UNPARSED_ENTITY_DECL));
    }

    public static boolean isNotationDecl(Node node) {
        return nameEquals(node, DTDConfiguration.E_NOTATION_DECL);
    }

    public static boolean isContentModel(Node node) {
        return nameEquals(node, DTDConfiguration.E_CONTENT_MODEL);
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
        entities = RubyHash.newHash(runtime);
        notations = RubyHash.newHash(runtime);
        contentModels = RubyHash.newHash(runtime);

        // recursively extract decls
        if (dtdNode == null) return; // leave all the decl hash's empty
        extractDecls(context, dtdNode);

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

        // add content models to the matching element decl
        keys = contentModels.keys();
        for (int i = 0; i < keys.getLength(); ++i) {
            IRubyObject key = keys.entry(i);
            IRubyObject cm = contentModels.op_aref(context, key);

            IRubyObject elem = elements.op_aref(context, key);
            if (elem.isNil()) continue;
            if (((XmlElementDecl)elem).isEmpty()) continue;
            ((XmlElementDecl) elem).setContentModel(cm);
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
                XmlEntityDecl decl = (XmlEntityDecl)
                    XmlEntityDecl.create(context, child);
                entities.op_aset(context, decl.node_name(context), decl);
                allDecls.append(decl);
            } else if (isNotationDecl(child)) {
                XmlNode tmp = (XmlNode)
                    XmlNode.constructNode(context.getRuntime(), child);
                IRubyObject decl = invoke(context, notationClass, "new",
                                          tmp.getAttribute(context, "name"),
                                          tmp.getAttribute(context, "pubid"),
                                          tmp.getAttribute(context, "sysid"));
                notations.op_aset(context,
                                  tmp.getAttribute(context, "name"), decl);
                allDecls.append(decl);
            } else if (isContentModel(child)) {
                XmlElementContent cm =
                    new XmlElementContent(context.getRuntime(),
                                          (XmlDocument) document(context),
                                          child);
                contentModels.op_aset(context, cm.element_name(context), cm);
            } else {
                // recurse
                extractDecls(context, child);
            }

            child = child.getNextSibling();
        }
    }

}
