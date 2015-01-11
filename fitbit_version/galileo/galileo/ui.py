"""\
This is where to look for for all user interaction stuff ...
"""

import sys

try:
    from html.parser import HTMLParser
except ImportError:
    # Python2
    from HTMLParser import HTMLParser

class Form(object):
    def __init__(self):
        self.fields = set()
        self.submit = None

    def addField(self, field):
        self.fields.add(field)

    def commonFields(self, answer, withValues=True):
        res = 0
        for field in self.fields:
            if field.name in answer:
                if withValues:
                    if field.value is not None and field.value == answer[field.name]:
                        res += 1
                else:
                    res += 1
        return res

    def takeValuesFromAnswer(self, answer):
        """\
        Transfer the answers from the config to the form
        """
        for field in self.fields:
            field.value = answer.get(field.name, field.value)
            if (field.name in answer) and (field.type == 'submit'):
                self.submit = field.name

    def asXML(self):
        """\
        Return the XML tuples. The trick is: THere can be only one 'submit'
        """
        res = []
        for field in self.fields:
            if field.type == 'submit':
                if self.submit != field.name:
                    continue
            res.append(field.asXMLParam())
        return res

    def __str__(self):
        return ', '.join(str(f) for f in self.fields)
    __repr__ = __str__  # To get it printed

    def asDict(self):
        """ for comparison in the test suites """
        return dict((f.name, f.value) for f in self.fields)

class FormField(object):
    def __init__(self, name, type='text', value=None, **kw):
        self.name = name
        self.type = type
        self.value = value

    def asXMLParam(self):
        return ('param', {'name': self.name}, [], self.value)

    def __str__(self):
        return '%r: %r' % (self.name, self.value)


class FormExtractor(HTMLParser):
    """ This read a whole html page and extract the forms """
    def __init__(self):
        self.forms = []
        self.curForm = None
        self.curSelect = None
        HTMLParser.__init__(self)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'form':
            self.curForm = Form()
        if tag == 'input':
            if 'name' in attrs:
                if self.curForm is None:
                    # In case the input happen outside of a form, just create
                    # one, and adds it immediatly
                    f = Form()
                    f.addField(FormField(**attrs))
                    self.forms.append(f)
                else:
                    self.curForm.addField(FormField(**attrs))

        if tag == 'select':
            self.curSelect = FormField(type='select', **attrs)
        if tag == 'option' and 'selected' in attrs:
            self.curSelect.value = attrs['value']


    def handle_endtag(self, tag):
        if tag == 'form':
            self.forms.append(self.curForm)
            self.curForm = None
        if tag == 'select':
            self.curForm.addField(self.curSelect)
            self.curSelect = None

    def handle_data(self, data): pass


class BaseUI(object):
    """\
    This is the base of all ui classes, it provides an interface and handy
    methods
    """
    def request(self, action, client_display):
        raise NotImplementedError

class MissingConfigError(Exception):
    def __init__(self, action, forms):
        self.action = action
        self.forms = forms
    def __str__(self):
        s = ["The server is asking a question to which I don't know any"
             " answer.",]
        s.append("Please add the section '%s' in the galileorc configuration"
                 " file under 'hardcoded-ui'" % self.action)
        s.append("Under this section, you should add the answer for one of the"
                 " following forms:")
        for f in self.forms:
            s.append(" - %s" % f.asDict())
        s.append("To help you decide, you can run the pairing process with the"
                 " `--debug` command line switch,")
        s.append("this will print the HTML code from which the questions have"
                 " been extracted.")
        return '\n'.join(s)

class HardCodedUI(BaseUI):
    """\
    This ui class doesn't show anything to the user and takes its answers
    from a list of hard-coded ones
    """
    def __init__(self, answers):
        self.answers = answers

    def request(self, action, html):
        if html.startswith('<![CDATA[') and html.endswith(']]>'):
            html = html[len('<![CDATA['):-len(']]>')]
        fe = FormExtractor()
        fe.feed(html)
        if action not in self.answers:
            raise MissingConfigError(action, fe.forms)
        answer = self.answers[action]
        # Figure out which of the form we should fill
        goodForm = None
        if len(fe.forms) == 1:
            # Only one there, no need to search for the correct one ...
            goodForm = fe.forms[0]
        else:
            # We need to find the one that match the most our answers
            max = 0
            for form in fe.forms:
                v = form.commonFields(answer)
                if v > max:
                    goodForm = form
                    max = v
            if max == 0:
                # Not found, search again, less picky
                for form in fe.forms:
                    v = form.commonFields(answer, False)
                    if v > max:
                        goodForm = form
                        max = v
        if goodForm is None:
            raise ValueError('no answer found')
        goodForm.takeValuesFromAnswer(answer)
        return goodForm.asXML()


def query_yes_no(question, default="y"):
    """Ask a yes/no question via raw_input() and return their answer.

    "question" is a string that is presented to the user.
    "default" is the presumed answer if the user just hits <Enter>.
        It must be "yes" (the default), "no" or None (meaning
        an answer is required of the user).

    The "answer" return value is one of True or False.

    This is from http://stackoverflow.com/a/3041990/1182619
    Itself from http://code.activestate.com/recipes/577058/
    """
    valid = {"yes":True,   "y":True,  "ye":True,
             "no":False,   "n":False}
    if default is None:
        prompt = " [y/n] "
    elif valid.get(default, False):
        prompt = " [Y/n] "
    elif not valid.get(default, True):
        prompt = " [y/N] "
    else:
        raise ValueError("invalid default answer: '%s'" % default)

    while True:
        sys.stdout.write(question + prompt)
        choice = raw_input().lower()
        if default is not None and choice == '':
            return valid[default]
        elif choice in valid:
            return valid[choice]
        else:
            sys.stdout.write("Please respond with 'yes' or 'no' "\
                             "(or 'y' or 'n').\n")


class InteractiveUI(HardCodedUI):
    """ We can't avoid asking the user to type what's written on the dongle """

    def request(self, action, html):
        if action == 'requestSecret':
            return self.handle_requestSecret()
        return HardCodedUI.request(self, action, html)

    def handle_requestSecret(self):
        if not query_yes_no("Do you see a number ?"):
            return [('param', {'name': 'secret'}, [], ''),
                     ('param', {'name': 'tryOther'}, [], 'TRY_OTHER')]
        sys.stdout.write("Type here the number you see:")
        secret = raw_input()
        return [('param', {'name': 'secret'}, [], secret)]
