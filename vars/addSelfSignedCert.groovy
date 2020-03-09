#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput;


def call(auth,serviceName,location,org,namespace,cluster) {

    //initial authentication
    def response = httpRequest httpMode: 'POST',
            url: 'https://portr.ctl.io/portr/authenticate',
            customHeaders:[[name:'Authorization', value:"Basic ${auth}"]]
    def json = jsonParse(response.content)
    def portrToken = "${json.token}"

    //request to get the cert
    response = httpRequest httpMode: 'GET',
            contentType: 'APPLICATION_JSON',
            validResponseCodes: '200:405',
            url: 'https://fastr.ctl.io/api/orgs/sso/mastrapplication/' + serviceName,
            customHeaders:[[name:'Authorization', value:"Bearer ${portrToken}"]]

    if (response.status == 404) {
        //create the cert
        def payload =" { " +
                " \"metadata\": {                                                   " +
                "    \"location\": \"" + location + "\",                            " +
                "    \"organization\": \"" + org + "\",                             " +
                "    \"resource\": \"mastrapplication\",                            " +
                "    \"name\": \"" + serviceName + "\"                              " +
                " },                                                                " +
                " \"spec\": {                                                       " +
                "   \"type\": \"self-signed-certificate\",                          " +
                "    \"cluster\": \"" + cluster + "\",                              " +
                "    \"namespace\": \"" + namespace + "\",                          " +
                "    \"durationDays\": 90,                                          " +
                "    \"renewalDays\": 30,                                           " +
                "    \"subject\": \"" + serviceName + ".sso-mastr.ctl.io\",         " +
                "    \"subjectAlternativeNames\": [                                 " +
                "        {                                                          " +
                "            \"type\": \"DNS\",                                     " +
                "            \"value\": \"" + serviceName + ".sso-mastr.ctl.io\"    " +
                "        }                                                          " +
                "    ]                                                              " +
                "}                                                                  " +
                "}"

        //request to get the records
        httpRequest httpMode: 'POST',
                contentType: 'APPLICATION_JSON',
                requestBody: payload,
                url: 'https://fastr.ctl.io/api/orgs/sso/mastrapplications',
                customHeaders:[[name:'Authorization', value:"Bearer ${portrToken}"]]
    }
}
