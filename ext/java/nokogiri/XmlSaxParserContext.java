package nokogiri;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import nokogiri.internals.NokogiriHandler;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

import static org.jruby.javasupport.util.RuntimeHelpers.invoke;

public class XmlSaxParserContext extends RubyObject {
    private InputSource source;
	private XMLReader reader;

    public XmlSaxParserContext(final Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);

        try {
            reader = XMLReaderFactory.createXMLReader();
		} catch (SAXException se) {
            throw RaiseException.createNativeRaiseException(ruby, se);
        }
    }

    @JRubyMethod(name="memory", meta=true)
    public static IRubyObject parse_memory(ThreadContext context, IRubyObject klazz, IRubyObject data) {
        ByteList byteList = data.convertToString().getByteList();
		ByteArrayInputStream bais = new ByteArrayInputStream(byteList.unsafeBytes(), byteList.begin(), byteList.length());

		XmlSaxParserContext ctx = new XmlSaxParserContext(context.getRuntime(), (RubyClass) klazz);

		ctx.source = new InputSource(bais);

		return ctx;
    }

    @JRubyMethod(name="file", meta=true)
    public static IRubyObject parse_file(ThreadContext context, IRubyObject klazz, IRubyObject data) {
        String filename = data.convertToString().asJavaString();

		XmlSaxParserContext ctx = new XmlSaxParserContext(context.getRuntime(), (RubyClass) klazz);

		try{
			ctx.source = new InputSource(new FileInputStream(filename));
		} catch (Exception ex) {
			throw RaiseException.createNativeRaiseException(context.getRuntime(), ex);
		}

		return ctx;
    }

    @JRubyMethod(name="io", meta=true)
    public static IRubyObject native_parse_io(ThreadContext context, IRubyObject klazz, IRubyObject data, IRubyObject enc) {
        Ruby ruby = context.getRuntime();
        int encoding = (int)enc.convertToInteger().getLongValue();

        if (invoke(context, data, "respond_to?",
                   ruby.newSymbol("to_io").to_sym()).isTrue()) {
            RubyIO io = (RubyIO)TypeConverter.convertToType(data, ruby.getIO(), "to_io");
            XmlSaxParserContext ctx = new XmlSaxParserContext(ruby, (RubyClass) klazz);

            ctx.source = new InputSource(io.getInStream());
            return ctx;
        } else if (invoke(context, data, "respond_to?",
                          ruby.newSymbol("string").to_sym()).isTrue()) {
            IRubyObject str = invoke(context, data, "string");
            return parse_memory(context, klazz, str);
        } else {
            throw ruby.newArgumentError("must respond to :to_io or :string");
        }
    }

	@JRubyMethod()
	public IRubyObject parse_with(ThreadContext context, IRubyObject handlerRuby) {
		Ruby ruby = context.getRuntime();

		if(!invoke(context, handlerRuby, "kind_of?",
				ruby.getClassFromPath("Nokogiri::XML::SAX::Parser")).isTrue()) {
			throw ruby.newArgumentError("argument must be a Nokogiri::XML::SAX::Parser");
		}

		DefaultHandler2 handler = new NokogiriHandler(ruby, handlerRuby);

		this.reader.setContentHandler(handler);
		this.reader.setErrorHandler(handler);

		try{
			this.reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
		} catch(Exception ex) {
			System.out.println("Problem while creating XML SAX Parser: "+ex.toString());
		}

		try{
            try {
                this.reader.parse(this.source);
            } catch(SAXParseException spe) {
                // A bad document (<foo><bar></foo>) should call the error handler instead of raising a
                // SAX exception.

                // However, an EMPTY document should raise a RuntimeError.  This is a bit kludgy, but
                // AFAIK SAX doesn't distinguish between empty and bad whereas Nokogiri does.
                String message = spe.getMessage();
                if ("Premature end of file.".matches(message)) {
                    throw context.getRuntime().newRuntimeError("couldn't parse document: "+ message);
                } else {
                    handler.error(spe);
                }

            }
		} catch(SAXException se) {
			throw RaiseException.createNativeRaiseException(ruby, se);
		} catch(IOException ioe) {
			throw ruby.newIOErrorFromException(ioe);
		}

		return this;
	}

    /**
     * Can take a boolean assignment.
     *
     * @param context
     * @param klazz
     * @param data
     * @return
     */
    @JRubyMethod(name = "replace_entities=")
    public IRubyObject set_replace_entities(ThreadContext context, IRubyObject value) {
        System.out.println("replace entities called with " + value.toString());
                        
        return this;
    }

}
