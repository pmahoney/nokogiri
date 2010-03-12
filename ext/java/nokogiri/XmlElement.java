/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nokogiri;

import nokogiri.internals.SaveContext;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author sergio
 */
public class XmlElement extends XmlNode {

    public XmlElement(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public XmlElement(Ruby runtime, RubyClass klazz, Node element) {
        super(runtime, klazz, element);
    }

    @Override
    @JRubyMethod
    public IRubyObject add_namespace_definition(ThreadContext context, IRubyObject prefix, IRubyObject href) {
        Element e = (Element) node;

        String pref = "xmlns";
        
        if(!prefix.isNil()) {
            pref += ":"+prefix.convertToString().asJavaString();
        }

        e.setAttribute(pref, href.convertToString().asJavaString());

        return super.add_namespace_definition(context, prefix, href);
    }

    @Override
    public void add_namespace_definitions(ThreadContext context, XmlNamespace ns, String prefix, String href) {
        Element e = (Element) node;
        e.setAttribute(prefix, href);

        updateNodeNamespaceIfNecessary(context, ns);
    }

    @Override
    public boolean isElement() { return true; }

    @Override
    public IRubyObject get(ThreadContext context, IRubyObject key) {
        String keyString = key.convertToString().asJavaString();
        Element element = (Element) node;
        String value = element.getAttribute(keyString);
        if(!value.equals("")){
            return context.getRuntime().newString(value);
        }
        return context.getRuntime().getNil();
    }

    @Override
    public IRubyObject key_p(ThreadContext context, IRubyObject k) {
        Ruby ruby = context.getRuntime();
        String key = k.convertToString().asJavaString();
        Element element = (Element) node;
        return ruby.newBoolean(element.hasAttribute(key));
    }

    @Override
    public IRubyObject op_aset(ThreadContext context, IRubyObject index,
                               IRubyObject val) {
        String key = index.convertToString().asJavaString();
        String value = val.convertToString().asJavaString();
        Element element = (Element) node;
        element.setAttribute(key, value);
        return this;
    }

    @Override
    public IRubyObject remove_attribute(ThreadContext context, IRubyObject name) {
        String key = name.convertToString().asJavaString();
        Element element = (Element) node;
        element.removeAttribute(key);
        return this;
    }

    @Override
    public void relink_namespace(ThreadContext context) {
        Element e = (Element) node;

        e.getOwnerDocument().renameNode(e, e.lookupNamespaceURI(e.getPrefix()), e.getNodeName());

        if(e.hasAttributes()) {
            NamedNodeMap attrs = e.getAttributes();

            for(int i = 0; i < attrs.getLength(); i++) {
                Attr attr = (Attr) attrs.item(i);
                String nsUri = "";
                String prefix = attr.getPrefix();
                String nodeName = attr.getNodeName();
                if("xml".equals(prefix)) {
                    nsUri = "http://www.w3.org/XML/1998/namespace";
                } else if("xmlns".equals(prefix) || nodeName.equals("xmlns")) {
                    nsUri = "http://www.w3.org/2000/xmlns/";
                } else {
                    nsUri = attr.lookupNamespaceURI(nodeName);
                }

                e.getOwnerDocument().renameNode(attr, nsUri, nodeName);

            }
        }

        if(e.hasChildNodes()) {
            ((XmlNodeSet) children(context)).relink_namespace(context);
        }
    }

    @Override
    public void saveContent(ThreadContext context, SaveContext ctx) {
        boolean format = ctx.format();

        Element e = (Element) node;

        if(format) {
            NodeList tmp = e.getChildNodes();
            for(int i = 0; i < tmp.getLength(); i++) {
                Node cur = tmp.item(i);
                if(cur.getNodeType() == Node.TEXT_NODE ||
                        cur.getNodeType() == Node.CDATA_SECTION_NODE ||
                        cur.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                    ctx.setFormat(false);
                    break;
                }
            }
        }

        ctx.append("<");
        ctx.append(e.getNodeName());
        this.saveNodeListContent(context, (RubyArray) attribute_nodes(context), ctx);

        if(e.getChildNodes() == null && !ctx.noEmpty()) {
            ctx.append("/>");
            ctx.setFormat(format);
            return;
        }

        ctx.append(">");

//        ctx.append(current.content(context).convertToString().asJavaString());

        XmlNodeSet children = (XmlNodeSet) children(context);

        if(!children.isEmpty()) {
            if(ctx.format()) ctx.append("\n");
            ctx.increaseLevel();
            this.saveNodeListContent(context, children, ctx);
            ctx.decreaseLevel();
            if(ctx.format()) ctx.append(ctx.getCurrentIndentString());
        }

        ctx.append("</");
        ctx.append(e.getNodeName());
        ctx.append(">");

        ctx.setFormat(format);
    }

    @Override
    public void saveContentAsHtml(ThreadContext context, SaveContext ctx) {

        Element e = (Element) node;


        ctx.append("<");
        ctx.append(e.getNodeName());
        this.saveNodeListContentAsHtml(context, (RubyArray) attribute_nodes(context), ctx);

        ctx.append(">");

        Node next = e.getFirstChild();
        Node parent = e.getParentNode();
        if(ctx.format() && next != null &&
                next.getNodeType() != Node.TEXT_NODE &&
                next.getNodeType() != Node.ENTITY_REFERENCE_NODE &&
                parent != null &&
                parent.getNodeName() != null &&
                parent.getNodeName().charAt(0) != 'p'){
            ctx.append("\n");
        }

        if(e.getChildNodes().getLength() == 0) {
            ctx.append("</");
            ctx.append(e.getNodeName());
            ctx.append(">");
            if(ctx.format() && next != null &&
                next.getNodeType() != Node.TEXT_NODE &&
                next.getNodeType() != Node.ENTITY_REFERENCE_NODE &&
                parent != null &&
                parent.getNodeName() != null &&
                parent.getNodeName().charAt(0) != 'p'){
                ctx.append("\n");
            }
            return;
        }

        XmlNodeSet children = (XmlNodeSet) children(context);

        if(!children.isEmpty()) {
            if(ctx.format() && next != null &&
                next.getNodeType() != Node.TEXT_NODE &&
                next.getNodeType() != Node.ENTITY_REFERENCE_NODE &&
                parent != null &&
                parent.getNodeName() != null &&
                parent.getNodeName().charAt(0) != 'p'){
                ctx.append("\n");
            }
            this.saveNodeListContentAsHtml(context, children, ctx);
            if(ctx.format() && next != null &&
                next.getNodeType() != Node.TEXT_NODE &&
                next.getNodeType() != Node.ENTITY_REFERENCE_NODE &&
                parent != null &&
                parent.getNodeName() != null &&
                parent.getNodeName().charAt(0) != 'p'){
                ctx.append("\n");
            }
        }

        ctx.append("</");
        ctx.append(e.getNodeName());
        ctx.append(">");

        if(ctx.format() && next != null &&
            next.getNodeType() != Node.TEXT_NODE &&
            next.getNodeType() != Node.ENTITY_REFERENCE_NODE &&
            parent != null &&
            parent.getNodeName() != null &&
            parent.getNodeName().charAt(0) != 'p'){
            ctx.append("\n");
        }
    }

}
