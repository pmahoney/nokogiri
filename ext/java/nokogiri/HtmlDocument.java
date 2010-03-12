package nokogiri;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import nokogiri.internals.HtmlDocumentImpl;
import nokogiri.internals.HtmlEmptyDocumentImpl;
import nokogiri.internals.HtmlParseOptions;
import nokogiri.internals.ParseOptions;
import nokogiri.internals.SaveContext;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.SAXException;

public class HtmlDocument extends XmlDocument {

    public HtmlDocument(Ruby ruby, RubyClass klazz, Document doc) {
        super(ruby, klazz, doc);
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject read_memory(ThreadContext context, IRubyObject cls, IRubyObject[] args) {
        
        Ruby ruby = context.getRuntime();
        Arity.checkArgumentCount(ruby, args, 4, 4);
        ParseOptions options = new HtmlParseOptions(args[3]);
        try {
            Document document;
            document = options.parse(args[0].convertToString().asJavaString());
            HtmlDocument doc = new HtmlDocument(ruby, (RubyClass)cls, document);
            doc.setUrl(args[1]);
            options.addErrorsIfNecessary(context, doc);
            return doc;
        } catch (ParserConfigurationException pce) {
            return options.getDocumentWithErrorsOrRaiseException(context, pce);
        } catch (SAXException saxe) {
            return options.getDocumentWithErrorsOrRaiseException(context, saxe);
        } catch (IOException ioe) {
            return options.getDocumentWithErrorsOrRaiseException(context, ioe);
        }
    }

    @JRubyMethod
    public static IRubyObject serialize(ThreadContext context, IRubyObject htmlDoc) {
        throw context.getRuntime().newNotImplementedError("not implemented");
    }

    @Override
    public void saveContent(ThreadContext context, SaveContext ctx) {
        Document doc = getDocument();
        DocumentType dtd = doc.getDoctype();

        if(dtd != null) {
            ctx.append("<!DOCTYPE ");
            ctx.append(dtd.getName());
            if(dtd.getPublicId() != null) {
                ctx.append(" PUBLIC ");
                ctx.appendQuoted(dtd.getPublicId());
                if(dtd.getSystemId() != null) {
                    ctx.append(" ");
                    ctx.appendQuoted(dtd.getSystemId());
                }
            } else if(dtd.getSystemId() != null) {
                ctx.append(" SYSTEM ");
                ctx.appendQuoted(dtd.getSystemId());
            }
            ctx.append(">\n");
        }

        this.saveNodeListContentAsHtml(context,
                (XmlNodeSet) this.children(context), ctx);
        
        ctx.append("\n");
    }

    @Override
    public void saveContentAsHtml(ThreadContext context, SaveContext ctx) {
        this.saveContent(context, ctx);
    }
}
