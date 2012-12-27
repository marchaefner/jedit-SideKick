This is a fork of the Subversion repository of the [jEdit][] SideKick plugin
at https://jedit.svn.sourceforge.net/svnroot/jedit/plugins/SideKick, built
with [svn2git][].

## Improvements

  * Always update the structure tree on buffer switch. Unless "Parse on
    Buffer Switch" has been chosen, the previous parsing result for the
    buffer is shown.
  * New "Parse on Buffer Load" option. This is a slight misnomer as
    parsing does not necessarily happen at load time, but when an unparsed
    buffer is shown. It "feels" like an on-load action for most
    situations, but parses only the current buffer if multiple files are
    opened at the same time.
  * Fix an issue where parser was invoked multiple times for a single
    parse request.
  * Fix multiple other issues where the SideKick dockable was not properly
    updated.

## Issues

The JavaSideKick plugin tries to initiate parsing by sending a
`SideKickUpdate` message if its properties have changed (e.g. "Sort by").
This does not work (anymore) as `SideKickUpdate` is intended to be send
after parsing.

Parsing results of another SideKick (of a different View) are not
immediately visible but require at least a buffer change. There is
currently no reasonable way to de/activate the parser of another SideKick.


[jEdit]: http://jedit.org/
[svn2git]: https://github.com/nirvdrum/svn2git/
