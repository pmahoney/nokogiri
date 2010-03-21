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
 * DTD element declaration.
 *
 * @author Patrick Mahoney <pat@polycrystal.org>
 */
public class XmlElementDecl extends XmlNode {

    public static RubyClass getRubyClass(Ruby ruby) {
        return (RubyClass)ruby.getClassFromPath("Nokogiri::XML::ElementDecl");
    }

    public XmlElementDecl(Ruby ruby, RubyClass klass) {
        super(ruby, klass);
        throw ruby.newRuntimeError("node required");
    }

    /**
     * Initialize based on an elementDecl node from a NekoDTD parsed
     * DTD.
     */
    public XmlElementDecl(Ruby ruby, RubyClass klass, Node elemDeclNode) {
        super(ruby, klass, elemDeclNode);
    }

    public static IRubyObject create(ThreadContext context, Node elemDeclNode) {
        XmlElementDecl self =
            new XmlElementDecl(context.getRuntime(),
                               getRubyClass(context.getRuntime()),
                               elemDeclNode);
        return self;
    }

    public IRubyObject element_name(ThreadContext context) {
        return getAttribute(context, "ename");
    }

    @Override
    @JRubyMethod
    public IRubyObject node_name(ThreadContext context) {
        return element_name(context);
    }

    @Override
    @JRubyMethod(name = "node_name=")
    public IRubyObject node_name_set(ThreadContext context, IRubyObject name) {
        throw context.getRuntime()
            .newRuntimeError("cannot change name of DTD decl");
    }

//     @JRubyMethod
//     public element_type(ThreadContext context) {
//     }
}
