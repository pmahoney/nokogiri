package nokogiri;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import nokogiri.internals.NokogiriUserDataHandler;
import nokogiri.internals.ParseOptions;
import nokogiri.internals.SaveContext;
import nokogiri.internals.XmlDtdParser;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XmlDocument extends XmlNode {
    /* UserData keys for storing extra info in the document node. */
    protected final static String DTD_INTERNAL_SUBSET = "DTD_INTERNAL_SUBSET";
    protected final static String DTD_EXTERNAL_SUBSET = "DTD_EXTERNAL_SUBSET";

    private static boolean substituteEntities = false;
    private static boolean loadExternalSubset = false; // TODO: Verify this.

    /** cache variables */
    protected IRubyObject encoding = null;
    protected IRubyObject url = null;

    public XmlDocument(Ruby ruby, Document document) {
        this(ruby, (RubyClass) ruby.getClassFromPath("Nokogiri::XML::Document"), document);
    }

    public XmlDocument(Ruby ruby, RubyClass klass, Document document) {
        super(ruby, klass, document);

//        if(document == null) {
//            this.internalNode = new XmlEmptyDocumentImpl(ruby, document);
//        } else {

//        }

        setInstanceVariable("@decorators", ruby.getNil());
    }

//     @Override
//     protected IRubyObject dup_implementation(ThreadContext context, boolean deep) {
//         return ((XmlDocumentImpl) this.internalNode).dup_impl(context, this, deep, this.getType());
//     }

    public Document getDocument() {
        return (Document) node;
    }

    protected void setUrl(IRubyObject url) {
        this.url = url;
    }

    protected IRubyObject getUrl() {
        return this.url;
    }

    @JRubyMethod
    public IRubyObject url(ThreadContext context) {
        return getUrl();
    }

    @JRubyMethod(name="new", meta = true, rest = true, required=0)
    public static IRubyObject rbNew(ThreadContext context, IRubyObject cls, IRubyObject[] args) {
        XmlDocument doc = null;
        try {
            Document docNode = (new ParseOptions(0)).getDocumentBuilder().newDocument();
            doc = new XmlDocument(context.getRuntime(), (RubyClass) cls,
                                  docNode);
        } catch (Exception ex) {
            throw context.getRuntime().newRuntimeError("couldn't create document: "+ex.toString());
        }

        RuntimeHelpers.invoke(context, doc, "initialize", args);

        return doc;
    }

    @Override
    @JRubyMethod
    public IRubyObject document(ThreadContext context) {
        return this;
    }

    @JRubyMethod(name="encoding=")
    public IRubyObject encoding_set(ThreadContext context, IRubyObject encoding) {
        this.encoding = encoding;
        return this;
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        if (this.encoding == null) {
            if (getDocument().getXmlEncoding() == null) {
                this.encoding = context.getRuntime().getNil();
            } else {
                this.encoding = context.getRuntime().newString(getDocument().getXmlEncoding());
            }
        }

        return this.encoding;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject load_external_subsets_set(ThreadContext context, IRubyObject cls, IRubyObject value) {
        XmlDocument.loadExternalSubset = value.isTrue();
        return context.getRuntime().getNil();
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject read_io(ThreadContext context, IRubyObject cls, IRubyObject[] args) {
        Ruby ruby = context.getRuntime();

        IRubyObject content = RuntimeHelpers.invoke(context, args[0], "read");
        args[0] = content;

        return read_memory(context, cls, args);



//        Arity.checkArgumentCount(ruby, args, 4, 4);
//        ParseOptions options = new ParseOptions(args[3]);
//        try {
//            Document document;
//            if (args[0] instanceof RubyIO) {
//                RubyIO io = (RubyIO)args[0];
//                document = options.parse(io.getInStream());
//                XmlDocument doc = new XmlDocument(ruby, (RubyClass)cls, document);
//                doc.setUrl(args[1]);
//                options.addErrorsIfNecessary(context, doc);
//                return doc;
//            } else {
//                throw ruby.newTypeError("Only IO supported for Document.read_io currently");
//            }
//        } catch (ParserConfigurationException pce) {
//            return options.getDocumentWithErrorsOrRaiseException(context, pce);
//        } catch (SAXException saxe) {
//            return options.getDocumentWithErrorsOrRaiseException(context, saxe);
//        } catch (IOException ioe) {
//            return options.getDocumentWithErrorsOrRaiseException(context, ioe);
//        }
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject read_memory(ThreadContext context, IRubyObject cls, IRubyObject[] args) {
        
        Ruby ruby = context.getRuntime();
        Arity.checkArgumentCount(ruby, args, 4, 4);
        ParseOptions options = new ParseOptions(args[3]);
        try {
            Document document;
            RubyString content = args[0].convertToString();
            ByteList byteList = content.getByteList();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteList.unsafeBytes(), byteList.begin(), byteList.length());
            document = options.getDocumentBuilder().parse(bais);
            XmlDocument doc = new XmlDocument(ruby, (RubyClass)cls, document);
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
    public IRubyObject root(ThreadContext context) {
        Node rootNode = getDocument().getDocumentElement();
        if (rootNode == null)
            return context.getRuntime().getNil();
        else
            return XmlNode.fromNodeOrCreate(context, rootNode);
    }

    @JRubyMethod(name="root=")
    public IRubyObject root_set(ThreadContext context, IRubyObject newRoot) {
        Node rootNode = asXmlNode(context, root(context)).node;
        fromNode(context, rootNode)
            .replace_node(context, newRoot);

        return newRoot;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject substitute_entities_set(ThreadContext context, IRubyObject cls, IRubyObject value) {
        XmlDocument.substituteEntities = value.isTrue();
        return context.getRuntime().getNil();
    }

    public IRubyObject getInternalSubset(ThreadContext context) {
        IRubyObject dtd =
            (IRubyObject) node.getUserData(DTD_INTERNAL_SUBSET);

        if (dtd != null)
            return dtd;

        if (getDocument().getDoctype() == null)
            return context.getRuntime().getNil();

        /* At this point, we have an unparsed DTD, so parse it and
         * save it back */
        String internalDTD =
            getDocument().getDoctype().getInternalSubset();
        Document dtdNode = XmlDtdParser.parse(internalDTD);
        dtd = new XmlDtd(context.getRuntime(), dtdNode);
        node.setUserData(DTD_INTERNAL_SUBSET, dtd, null);

        return dtd;
    }


    @Override
    public void saveContent(ThreadContext context, SaveContext ctx) {
        if(!ctx.noDecl()) {
            ctx.append("<?xml version=\"");
            ctx.append(getDocument().getXmlVersion());
            ctx.append("\"");
//            if(!cur.encoding(context).isNil()) {
//                ctx.append(" encoding=");
//                ctx.append(cur.encoding(context).asJavaString());
//            }

            String encoding = ctx.getEncoding();

            if(encoding == null &&
                    !encoding(context).isNil()) {
                encoding = encoding(context).convertToString().asJavaString();
            }

            if(encoding != null) {
                ctx.append(" encoding=\"");
                ctx.append(encoding);
                ctx.append("\"");
            }

            ctx.append(" standalone=\"");
            ctx.append(getDocument().getXmlStandalone() ? "yes" : "no");
            ctx.append("\"?>\n");
        }

        IRubyObject maybeRoot = root(context);
        if (maybeRoot.isNil())
            throw context.getRuntime().newRuntimeError("no root document");

        XmlNode root = (XmlNode) maybeRoot;
        root.saveContent(context, ctx);
        ctx.append("\n");
    }
}
