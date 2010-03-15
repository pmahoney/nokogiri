package nokogiri;

import java.io.InputStream;
import java.io.IOException;

import nokogiri.internals.NokogiriHandler;
import org.cyberneko.html.parsers.SAXParser;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXNotRecognizedException;

import static org.jruby.javasupport.util.RuntimeHelpers.invoke;
import static nokogiri.internals.NokogiriHelpers.rubyStringToString;

public class HtmlSaxParserContext extends XmlSaxParserContext {
    private SAXParser parser;

    public HtmlSaxParserContext(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);

        this.parser = new SAXParser();

        try{
            this.parser.setProperty(
                "http://cyberneko.org/html/properties/names/elems", "match");
            this.parser.setProperty(
                "http://cyberneko.org/html/properties/names/attrs", "no-change");
        } catch(Exception ex) {
            throw ruby.newRuntimeError(
                "Problem while creating HTML SAX Parser: " + ex.toString());
        }

    }

    @JRubyMethod(name="memory", meta=true)
    public static IRubyObject parse_memory(ThreadContext context,
                                           IRubyObject klazz,
                                           IRubyObject data,
                                           IRubyObject encoding) {
        HtmlSaxParserContext ctx =
            new HtmlSaxParserContext(context.getRuntime(), (RubyClass) klazz);
        ctx.setInputSource(context, data);
        return ctx;
    }

    @JRubyMethod(name="file", meta=true)
    public static IRubyObject parse_file(ThreadContext context,
                                         IRubyObject klazz,
                                         IRubyObject data,
                                         IRubyObject encoding) {
        HtmlSaxParserContext ctx =
            new HtmlSaxParserContext(context.getRuntime(), (RubyClass) klazz);
        ctx.setInputSourceFile(context, data);
        return ctx;
    }

    @JRubyMethod(name="io", meta=true)
    public static IRubyObject parse_io(ThreadContext context,
                                       IRubyObject klazz,
                                       IRubyObject data,
                                       IRubyObject enc) {
        //int encoding = (int)enc.convertToInteger().getLongValue();
        HtmlSaxParserContext ctx =
            new HtmlSaxParserContext(context.getRuntime(), (RubyClass) klazz);
        ctx.setInputSource(context, data);
        return ctx;
    }

    /**
     * Create a new parser context that will read from a raw input
     * stream. Not a JRuby method.  Meant to be run in a separate
     * thread by XmlSaxPushParser.
     */
    public static IRubyObject parse_stream(ThreadContext context,
                                           IRubyObject klazz,
                                           InputStream stream) {
        XmlSaxParserContext ctx =
            new XmlSaxParserContext(context.getRuntime(), (RubyClass)klazz);
        ctx.setInputSource(stream);
        return ctx;
    }

    @Override
    protected void do_parse() throws SAXException, IOException {
        parser.parse(source);
    }

    @Override
    protected void setProperty(String key, Object val)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        parser.setProperty(key, val);
    }

    @Override
    protected void setContentHandler(ContentHandler handler) {
        parser.setContentHandler(handler);
    }

    @Override
    protected void setErrorHandler(ErrorHandler handler) {
        parser.setErrorHandler(handler);
    }

}
