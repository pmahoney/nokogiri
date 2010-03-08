package nokogiri;

import nokogiri.internals.XmlNodeImpl;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * DTD entity declaration.
 *
 * @author Patrick Mahoney <pat@polycrystal.org>
 */
public class XmlEntityDecl extends XmlNode {
    protected Node node = null;

    public static RubyClass getRubyClass(Ruby ruby) {
        return (RubyClass)ruby.getClassFromPath("Nokogiri::XML::EntityDecl");
    }

    public XmlEntityDecl(Ruby ruby, RubyClass klass) {
        super(ruby, klass);
        throw ruby.newRuntimeError("node required");
    }

    /**
     * Initialize based on an entityDecl node from a NekoDTD parsed
     * DTD.
     */
    public XmlEntityDecl(Ruby ruby, RubyClass klass, Node entDeclNode) {
        super(ruby, klass);
        node = entDeclNode;
        // TODO: ditch the *Impl classes?
        internalNode = new XmlNodeImpl(ruby, node);
    }

    public static IRubyObject create(ThreadContext context, Node entDeclNode) {
        XmlElementDecl self =
            new XmlElementDecl(context.getRuntime(),
                               getRubyClass(context.getRuntime()),
                               entDeclNode);
        return self;
    }

    protected IRubyObject getAttribute(ThreadContext context, String key) {
        return context.getRuntime().newString(((Element)node).getAttribute(key));
    }
}
