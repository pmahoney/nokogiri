package nokogiri;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.javasupport.util.RuntimeHelpers.invoke;

public class HtmlElementDescription extends RubyObject {

    public HtmlElementDescription(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    @JRubyMethod(name="[]", meta=true)
    public static IRubyObject get(ThreadContext context,
                                  IRubyObject klazz, IRubyObject key) {
        return context.getRuntime().getNil();
    }

}
