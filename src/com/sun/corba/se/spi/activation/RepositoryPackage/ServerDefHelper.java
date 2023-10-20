package com.sun.corba.se.spi.activation.RepositoryPackage;


/**
* com/sun/corba/se/spi/activation/RepositoryPackage/ServerDefHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from c:/jenkins/workspace/8-2-build-windows-amd64-cygwin/jdk8u281/880/corba/src/share/classes/com/sun/corba/se/spi/activation/activation.idl
* Wednesday, December 9, 2020 1:55:54 PM UTC
*/

abstract public class ServerDefHelper
{
  private static String  _id = "IDL:activation/Repository/ServerDef:1.0";

  public static void insert (org.omg.CORBA.Any a, com.sun.corba.se.spi.activation.RepositoryPackage.ServerDef that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static com.sun.corba.se.spi.activation.RepositoryPackage.ServerDef extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  private static boolean __active = false;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      synchronized (org.omg.CORBA.TypeCode.class)
      {
        if (__typeCode == null)
        {
          if (__active)
          {
            return org.omg.CORBA.ORB.init().create_recursive_tc ( _id );
          }
          __active = true;
          org.omg.CORBA.StructMember[] _members0 = new org.omg.CORBA.StructMember [5];
          org.omg.CORBA.TypeCode _tcOf_members0 = null;
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[0] = new org.omg.CORBA.StructMember (
            "applicationName",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[1] = new org.omg.CORBA.StructMember (
            "serverName",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[2] = new org.omg.CORBA.StructMember (
            "serverClassPath",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[3] = new org.omg.CORBA.StructMember (
            "serverArgs",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[4] = new org.omg.CORBA.StructMember (
            "serverVmArgs",
            _tcOf_members0,
            null);
          __typeCode = org.omg.CORBA.ORB.init ().create_struct_tc (com.sun.corba.se.spi.activation.RepositoryPackage.ServerDefHelper.id (), "ServerDef", _members0);
          __active = false;
        }
      }
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static com.sun.corba.se.spi.activation.RepositoryPackage.ServerDef read (org.omg.CORBA.portable.InputStream istream)
  {
    com.sun.corba.se.spi.activation.RepositoryPackage.ServerDef value = new com.sun.corba.se.spi.activation.RepositoryPackage.ServerDef ();
    value.applicationName = istream.read_string ();
    value.serverName = istream.read_string ();
    value.serverClassPath = istream.read_string ();
    value.serverArgs = istream.read_string ();
    value.serverVmArgs = istream.read_string ();
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, com.sun.corba.se.spi.activation.RepositoryPackage.ServerDef value)
  {
    ostream.write_string (value.applicationName);
    ostream.write_string (value.serverName);
    ostream.write_string (value.serverClassPath);
    ostream.write_string (value.serverArgs);
    ostream.write_string (value.serverVmArgs);
  }

}
