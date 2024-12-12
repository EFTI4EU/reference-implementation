<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:efti="http://efti.eu/v1/consignment/common"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"

  xsi:schemaLocation="http://efti.eu/v1/consignment/common ../consignment-common.xsd">
  <xsl:output method="html" encoding="UTF-8" indent="yes"/>
  <xsl:template match="/">
    <style>
      table {
      border-collapse: collapse;
      }

      td {
      padding:10px;
      }

      #inner thead td {
      font-size: 12px;
      }
      div{visibility: hidden;}
    </style>

    <table STYLE="border:1px inset black" width="843px" xmlns="http://www.w3.org/TR/xhtml1/strict">

      <thead>
        <tr>
          <td STYLE="border:1px inset black" width="400px">
            <strong>1. SENDER</strong>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:consignor/efti:postalAddress/efti:buildingNumber"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:consignor/efti:postalAddress/efti:streetName"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:consignor/efti:postalAddress/efti:postcode"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:consignor/efti:postalAddress/efti:cityName"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:consignor/efti:postalAddress/efti:countrySubDivisionName"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:consignor/efti:name"/>
            <br/>
          </td>
          <td STYLE="border:1px inset black" width="400px">
            <strong STYLE="font-size:18pt">
              EFTI
            </strong>
            <br/>
            CONSIGNMENT NOTE
            <br/>
            <br/>
          </td>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td STYLE="border:1px inset black" width="400px">
            <strong>2. CONSIGNEE</strong>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:consignee/efti:postalAddress/efti:buildingNumber"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:consignee/efti:postalAddress/efti:streetName"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:consignee/efti:postalAddress/efti:postcode"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:consignee/efti:postalAddress/efti:cityName"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:consignee/efti:postalAddress/efti:countrySubDivisionName"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:consignee/efti:name"/>
            <br/>

          </td>
          <td STYLE="border:1px inset black" width="400px">
            <strong>16. CARRIER</strong>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:carrier/efti:name"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:carrier/efti:postalAddress/efti:buildingNumber"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:carrier/efti:postalAddress/efti:streetName"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:carrier/efti:postalAddress/efti:postcode"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:carrier/efti:postalAddress/efti:cityName"/>
            <br/>
            <xsl:value-of select="/efti:consignment/carrier/efti:postalAddress/efti:countrySubDivisionName"/>
            <br/>

          </td>
        </tr>
        <tr>
          <td STYLE="border:1px inset black" width="400px">
            <strong>3. DELIVERY ADDRESS</strong>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:consigneeReceiptLocation/efti:postalAddress/efti:buildingNumber"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:consigneeReceiptLocation/efti:postalAddress/efti:streetName"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:consigneeReceiptLocation/efti:postalAddress/efti:postcode"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:consigneeReceiptLocation/efti:postalAddress/efti:cityName"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:consigneeReceiptLocation/efti:postalAddress/efti:countrySubDivisionName"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:consigneeReceiptLocation/efti:name"/>
            <br/>
          </td>
          <td STYLE="border:1px inset black" width="400px">
            <strong>17. Folowing carrier</strong>
            <br/>


            <br/>

            <br/>
            <br/>

          </td>
        </tr>
        <tr>
          <td STYLE="border:1px inset black" width="400px">
            <strong>4. Place and date of taking over of the goods</strong>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:carrierAcceptanceDateTime"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:carrierAcceptanceLocation/efti:postalAddress/efti:buildingNumber"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:carrierAcceptanceLocation/efti:postalAddress/efti:streetName"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:carrierAcceptanceLocation/efti:postalAddress/efti:postcode"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="/efti:consignment/efti:carrierAcceptanceLocation/efti:postalAddress/efti:cityName"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:carrierAcceptanceLocation/efti:postalAddress/efti:countrySubDivisionName"/>
            <br/>
            <xsl:value-of select="/efti:consignment/efti:carrierAcceptanceLocation/efti:name"/>
            <br/>
          </td>
          <td style="border: 1px inset black;" rowspan="2" width="400px">
            <strong>18.Carrier's reservations and observations</strong>
            <br/>
          </td>
        </tr>
        <tr>
          <td STYLE="border:1px inset black" width="400px">
            <strong>5. Annexed documents</strong>
            <br/>
          </td>
        </tr>

      </tbody>
    </table>

    <table id="inner" width="843px" style="border-right: 1px inset black;border-left: 1px inset black;"
           xmlns="http://www.w3.org/TR/xhtml1/strict" xmlns:ns0="http://dmc.sis.lt/dmc/schemas/edidoc">
      <thead>
        <td>6. Marks and numbers</td>
        <td>7. Number of packages</td>
        <td>8. Description of packing</td>
        <td>9. Nature of goods, Shipping name</td>
        <td>10. Statist. number</td>
        <td>11. Gross weight in kg</td>
        <td>12. Volume in m3</td>
      </thead>


      <tbody>
        <tr>
          <td>1.</td><td>4</td><td>truc cool</td><td>Moteur Harley</td><td></td><td>200KG</td><td>200m3</td>
        </tr>

        <tr>
          <td>2.</td>
          <td>4</td>
          <td>mega truc cool</td>
          <td>Poney Hollandais courte patte</td>
          <td></td>
          <td>1200KG</td>
          <td>500m3</td>
        </tr>

        <tr>
          <td>
            <b>Total:</b>
          </td>
          <td>
            <b>
              8
              Item
              <br/>
            </b>
          </td>
          <td>
            <b>
              <xsl:value-of select="/efti:consignment/efti:grossWeight"/>
              <xsl:text> </xsl:text>
              <xsl:value-of select="/efti:consignment/efti:grossWeight/@unitId"/>
            </b>
          </td>
          <td>
            <b>
              <xsl:value-of select="/efti:consignment/efti:grossVolume"/>
              <xsl:text> </xsl:text>
              <xsl:value-of select="/efti:consignment/efti:grossVolume/@unitId"/>
            </b>
          </td>
        </tr>
      </tbody>
    </table>


    <table width="843px" style="border: 1px inset black;" xmlns="http://www.w3.org/TR/xhtml1/strict"
           xmlns:ns0="http://dmc.sis.lt/dmc/schemas/edidoc">

      <tr>
        <td STYLE="border:1px inset black" width="400px">
          <strong>13. Sender's instructions (Custom and other formalities)</strong>

          <br></br>
          <br></br>
        </td>
        <td STYLE="border:1px inset black" width="400px">
          <strong>19.To be paid by :</strong>
          <br>
            <br></br>
          </br>
        </td>
      </tr>
      <tr>
        <td>
          <strong>14. Cash on delivery</strong>
        </td>
      </tr>
      <tr>
        <td height="30px" style="border-bottom: 1px inset black;"/>
      </tr>
      <tr>
        <td STYLE="border:1px inset black" width="400px">
          <strong>15. Directions as to freight payment</strong>

          <br></br>
        </td>
        <td STYLE="border:1px inset black" width="400px">
          <strong>20. Special agreements</strong>
          <br></br>
        </td>
      </tr>
      <tr>
        <td>
          <strong>21. Date and place of issue</strong>
        </td>
        <td>
        </td>
      </tr>
      <tr>
        <td height="30px" style="border-bottom: 1px inset black;"/>
      </tr>


    </table>

    <table xmlns="http://www.w3.org/TR/xhtml1/strict" xmlns:ns0="http://dmc.sis.lt/dmc/schemas/edidoc"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:rsm="urn:un:unece:uncefact:data:standard:eCMR:1"
           xmlns:qdt="urn:un:unece:uncefact:data:Standard:QualifiedDataType:101"
           xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:25"
           xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:101" id="inner"
           width="843px" style="border-right: 1px inset black;border-left: 1px inset black;">
      <thead>
        <td style="border-right: 1px inset black;border-left: 1px inset black">
          22. Signature and stamp of sender
          <br></br>
          <br></br>
        </td>
        <td style="border-right: 1px inset black">
          23. Signature and stamp of carrier
          <br></br>
          <br></br>
        </td>
        <td style="border-right: 1px inset black">
          24. Goods received
          <br></br>
          <br></br>
        </td>

      </thead>
      <tbody>
        <tr>
          <td style="border-right: 1px inset black">
            <br></br>
            <br></br>
          </td>
          <td style="border-right: 1px inset black">
            <br></br>
            <br></br>
          </td>
          <td style="border-right: 1px inset black">
            <br></br>
            <br></br>
          </td>

        </tr>

      </tbody>
    </table>
    <table style="border: 1px inset black;" xmlns="http://www.w3.org/TR/xhtml1/strict"
           xmlns:ns0="http://dmc.sis.lt/dmc/schemas/edidoc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:rsm="urn:un:unece:uncefact:data:standard:eCMR:1"
           xmlns:qdt="urn:un:unece:uncefact:data:Standard:QualifiedDataType:101"
           xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:25"
           xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:101" id="inner"
           width="843px">
      <thead>
        <td style="border-right: 1px inset black">
          25. Vehicle’s and trailer’s number
          <br></br>
          <br></br>
        </td>
        <td style="border-right: 1px inset black">
          26. Vehicle’s and trailer’s model
          <br></br>
          <br></br>
        </td>
        <td style="border-right: 1px inset black">
          27. Tarif
          <br></br>
          <br></br>
        </td>
      </thead>
      <tbody>
        <tr>
          <td style="border-right: 1px inset black">
            <br></br>
            <br></br>
          </td>
          <td style="border-right: 1px inset black">
            <br></br>
            <br></br>
          </td>
          <td style="border-right: 1px inset black">
            <br></br>
            <br></br>
          </td>

        </tr>

      </tbody>
    </table>


    <table width="843px" style="border: 1px inset black;" xmlns="http://www.w3.org/TR/xhtml1/strict"
           xmlns:ns0="http://dmc.sis.lt/dmc/schemas/edidoc">
      <tr>
        <td>
          <strong>28. Tarifs</strong>
        </td>
      </tr>
      <tr>
        <td height="30px" style="border-bottom: 1px inset black;"/>
      </tr>
    </table>


  </xsl:template>

</xsl:stylesheet>
