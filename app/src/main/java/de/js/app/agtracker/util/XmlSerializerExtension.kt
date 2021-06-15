package de.js.app.agtracker.util

import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter

//http://blog.agileactors.com/blog/2018/2/7/how-to-generate-xml-with-kotlin-extension-functions-and-lambdas-in-android-app

//  XML generation by code
fun XmlSerializer.document(
    docName: String = "UTF-8",
    xmlStringWriter: StringWriter = StringWriter(),
    init: XmlSerializer.() -> Unit
): String {
    startDocument(docName, true)
    xmlStringWriter.buffer.setLength(0) //  refreshing string writer due to reuse
    setOutput(xmlStringWriter)
    //Test = xmlStringWriter
    init()
    endDocument()
    return xmlStringWriter.toString()
}

//  element
fun XmlSerializer.element(namespace: String = "", name: String, init: XmlSerializer.() -> Unit) {
    startTag(namespace, name)
    init()
    endTag(namespace, name)
}

//  element with attribute & content
fun XmlSerializer.element(
    namespace: String = "",
    name: String,
    content: String,
    init: XmlSerializer.() -> Unit
) {
    startTag(namespace, name)
    init()
    text(content)
    endTag(namespace, name)
}

fun XmlSerializer.elementCdsect(
    namespace: String = "",
    name: String,
    content: String,
    init: XmlSerializer.() -> Unit
) {
    startTag(namespace, name)
    init()
    cdsect(content)
    endTag(namespace, name)
}

//  element with content
fun XmlSerializer.element(namespace: String = "", name: String, content: String) =
    element(namespace, name) {
        text(content)
    }

//  attribute
fun XmlSerializer.attribute(namespace: String = "", name: String, value: String) =
    attribute(namespace, name, value)



