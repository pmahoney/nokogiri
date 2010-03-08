package nokogiri;

import java.util.ArrayList;

import nokogiri.internals.XmlNodeImpl;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * DTD attribute declaration.  This extends XmlNode but the
 * implementation breaks the XmlNode convention of a separate
 * XmlAttributeDeclImpl class.  Thus many XmlNode methods can be
 * expected to fail...
 *
 * TODO: either convert to the XmlNode convention or change XmlNode
 * and descendents to follow XmlAttributeDecl's (simpler?) convention.
 *
 * @author Patrick Mahoney <pat@polycrystal.org>
 */
public class XmlAttributeDecl extends XmlNode {
    protected Node node = null;

    public static RubyClass getRubyClass(Ruby ruby) {
        return (RubyClass)ruby.getClassFromPath("Nokogiri::XML::AttributeDecl");
    }

    public XmlAttributeDecl(Ruby ruby, RubyClass klass) {
        super(ruby, klass);
        throw ruby.newRuntimeError("node required");
    }

    /**
     * Initialize based on an attributeDecl node from a NekoDTD parsed
     * DTD.
     *
     * Internally, XmlAttributeDecl combines these into a single node.
     */
    public XmlAttributeDecl(Ruby ruby, RubyClass klass, Node attrDeclNode) {
        super(ruby, klass);
        node = attrDeclNode;
        // TODO: ditch the *Impl classes?
        internalNode = new XmlNodeImpl(ruby, node);
    }

    public static IRubyObject create(ThreadContext context, Node attrDeclNode) {
        XmlAttributeDecl self =
            new XmlAttributeDecl(context.getRuntime(),
                                 getRubyClass(context.getRuntime()),
                                 attrDeclNode);
        return self;
    }

    protected IRubyObject getAttribute(ThreadContext context, String key) {
        return context.getRuntime().newString(((Element)node).getAttribute(key));
    }

    @JRubyMethod
    public IRubyObject attribute_type(ThreadContext context) {
        return getAttribute(context, "atype");
    }

    @JRubyMethod(name="default")
    public IRubyObject default_value(ThreadContext context) {
        return getAttribute(context, "default");
    }

    @JRubyMethod
    public IRubyObject enumeration(ThreadContext context) {
        throw context.getRuntime().newNotImplementedError("not implemented");
    }
}

