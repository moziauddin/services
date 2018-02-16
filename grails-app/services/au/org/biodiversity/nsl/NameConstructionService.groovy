/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL services project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package au.org.biodiversity.nsl

import grails.util.Holders

class NameConstructionService {

    def configService
    def classificationService
    def shardCode
    def nameConstructor
    {

        shardCode = configService.getShardCode().toLowerCase()
        // gets the appropiate name constructor service for the shard code ( e.g. ICN , ICZN )
        nameConstructor = Holders.grailsApplication.mainContext.getBean(shardCode+'NameConstructionService')
    }


    static transactional = false

    static String stripMarkUp(String string) {
        string?.replaceAll(/<[^>]*>/, '')?.replaceAll(/(&lsquo;|&rsquo;)/, "'")?.decodeHTML()?.trim()
    }

    static String join(List<String> bits) {
        bits.findAll { it }.join(' ')
    }

    /**
     * Make the sortName from the passed in simple name and name object.
     * We pass in simple name because it may not have been set on the name yet for new names.
     * NSL-1837
     *
     * @param name
     * @param simpleName
     * @return sort name string
     */
    String makeSortName(Name name, String simpleName) {

        return nameConstructor.makeSortName(name, simpleName)
    }

    Map constructName(Name name) {
        return nameConstructor.constructName(name)
    }



    String makeConnectorString(Name name, String rank) {
        return nameConstructor.makeConnectorString(name, rank)
    }

    String makeRankString(Name name) {
        return nameConstructor.makeRankString(name)
    }

    String constructAuthor(Name name) {
        return nameConstructor.constructAuthor(name)
    }
}

