package nokogiri;

import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.RubyArray;
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
        super(ruby, klass, attrDeclNode);
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

    /**
     * FIXME: will enumerations all be of the simple (val1|val2|val3)
     * type string?
     */
    @JRubyMethod
    public IRubyObject enumeration(ThreadContext context) {
        RubyArray enumVals = RubyArray.newArray(context.getRuntime());
        String atype = ((Element)node).getAttribute("atype");

        if (atype != null && atype.charAt(0) == '(') {
            // removed enclosing parens
            String valueStr = atype.substring(1, atype.length() - 1);
            String[] values = valueStr.split("\\|");
            for (int i = 0; i < values.length; i++) {
                enumVals.append(context.getRuntime().newString(values[i]));
            }
        }

        return enumVals;
    }
}
