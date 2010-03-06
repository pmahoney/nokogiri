package nokogiri;

import java.io.InputStream;
import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.Runnable;
import java.lang.Thread;
import nokogiri.internals.PushInputStream;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.xml.sax.InputSource;

import static org.jruby.javasupport.util.RuntimeHelpers.invoke;

public class XmlSaxPushParser extends RubyObject {
    PushInputStream stream;
    Thread reader;

    public XmlSaxPushParser(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    @JRubyMethod
    public IRubyObject initialize_native(final ThreadContext context,
                                         IRubyObject _saxParser,
                                         IRubyObject fileName) {
        stream = new PushInputStream();

        Runner runner = new Runner(context, this, stream);

        reader = new Thread(runner);
        reader.start();

        return this;
    }

    @JRubyMethod
    public IRubyObject native_write(ThreadContext context, IRubyObject chunk,
                                    IRubyObject isLast) {

        byte[] data = chunk.toString().getBytes();
        stream.writeAndWaitForRead(data);
        if (isLast.isTrue()) {
            try {
                stream.close();
            } catch (IOException e) {
                // ignore
            }

            for (;;) {
                try {
                    reader.join();
                    break;
                } catch (InterruptedException e) {
                    // continue loop
                }
            }
        }
        return this;
    }

    protected static class Runner implements Runnable {
        protected ThreadContext context;
        protected IRubyObject handler;
        protected XmlSaxParserContext parser;

        public Runner(ThreadContext context,
                      IRubyObject handler,
                      InputStream stream) {
            RubyClass klazz = (RubyClass) context.getRuntime()
                .getClassFromPath("Nokogiri::XML::SAX::ParserContext");

            this.context = context;
            this.handler = handler;
            this.parser = (XmlSaxParserContext)
                XmlSaxParserContext.parse_stream(context, klazz, stream);
        }

        public void run() {
            parser.parse_with(context, handler);
        }
    }
}
