package nokogiri;

import nokogiri.internals.NokogiriHelpers;
import nokogiri.internals.SaveContext;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XmlText extends XmlNode {
    public XmlText(Ruby ruby, RubyClass rubyClass, Node node) {
        super(ruby, rubyClass, node);
    }

    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject rbNew(ThreadContext context, IRubyObject cls, IRubyObject text, IRubyObject xNode) {
        XmlNode xmlNode = (XmlNode)xNode;
        XmlDocument xmlDoc = (XmlDocument)xmlNode.document(context);
        Document document = xmlDoc.getDocument();
        Node node = document.createTextNode(text.convertToString().asJavaString());
        return XmlNode.constructNode(context.getRuntime(), node);
    }

    @Override
    public void saveContent(ThreadContext context, SaveContext ctx) {
        if(ctx.format()) {
            ctx.append(ctx.getCurrentIndentString());
        }

        ctx.append(NokogiriHelpers.encodeJavaString(
                content(context).convertToString().asJavaString()
                ));
    }

    @Override
    public void saveContentAsHtml(ThreadContext context, SaveContext ctx) {
        ctx.append(NokogiriHelpers.encodeJavaString(
                content(context).convertToString().asJavaString()
                ));
    }

}
