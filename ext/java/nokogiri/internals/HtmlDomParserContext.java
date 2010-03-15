package nokogiri.internals;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import nokogiri.HtmlDocument;
import nokogiri.XmlDocument;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author sergio
 */
public class HtmlDomParserContext extends XmlDomParserContext {

    public HtmlDomParserContext(Ruby runtime, IRubyObject options) {
        super(runtime, options);
    }

    public HtmlDomParserContext(Ruby runtime, long options) {
        super(runtime, options);
    }

    @Override
    protected XmlDocument getNewEmptyDocument(ThreadContext context) {
        IRubyObject[] args = new IRubyObject[0];
        return (XmlDocument) XmlDocument.rbNew(context,
                    context.getRuntime().getClassFromPath("Nokogiri::XML::Document"),
                    args);
    }

    @Override
    protected XmlDocument wrapDocument(ThreadContext context,
                                       RubyClass klass,
                                       Document doc) {
        return new HtmlDocument(context.getRuntime(), klass, doc);
    }


    @Override
    public Document do_parse()
        throws ParserConfigurationException, SAXException, IOException {
        XMLParserConfiguration config = new HTMLConfiguration();
        config.setProperty("http://cyberneko.org/html/properties/names/elems",
                           "lower");
        config.setProperty("http://cyberneko.org/html/properties/names/attrs",
                           "lower");

        DOMParser parser = new DOMParser(config);
        parser.setFeature("http://xml.org/sax/features/namespaces", false);
//        parser.setProperty("http://cyberneko.org/html/properties/filters",
//              new XMLDocumentFilter[] { new DefaultFilter() {
//                  @Override
//                  public void startElement(QName element, XMLAttributes attrs,
//                                         Augmentations augs) throws XNIException {
//                  element.uri = null;
//                  super.startElement(element, attrs, augs);
//                }
//              }});

        parser.parse(getInputSource());
        return parser.getDocument();
    }
}
