package net.duboue.reveng.javadoc;

/*
Copyright (c) 2012 Pablo Ariel Duboue <pablo.duboue@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.File;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;

public class DumpComments {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (String file : args) {
			try {
				JavaDocBuilder builder = new JavaDocBuilder();
				builder.addSource(new File(file));
				JavaSource src = builder.getSources()[0];
				// String pkg = src.getPackageName();
				for (JavaClass javaClass : src.getClasses())
					for (JavaMethod method : javaClass.getMethods()) {
						String comment = method.getComment();
						comment = comment == null ? "" : comment.replaceAll("\n", " ");
						System.out.println(javaClass.getFullyQualifiedName()
								+ "\t"
								+ method.getName() //
								+ "\t"
								+ method.getDeclarationSignature(true) //
								+ "\t"
								+ comment);
					}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
}
