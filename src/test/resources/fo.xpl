<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                version="3.0">

<p:output port="result"/>

<p:css-formatter>
  <p:with-input port="source">
    <p:document href="envelope.fo"/>
  </p:with-input>
</p:css-formatter>

</p:declare-step>
