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

class IcnNameConstructionService {

    def classificationService
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

        String abbrev = name.nameRank.abbrev
        String sortName = simpleName.toLowerCase()
                .replaceAll(/^x /, '') //remove hybrid marks
                .replaceAll(/ [x+-] /, ' ') //remove hybrid marks
                .replaceAll(" $abbrev ", ' ') //remove rank abreviations
                .replaceAll(/ MS$/, '') //remove manuscript
        return sortName
    }

    Map constructName(Name name) {
        if (!name) {
            throw new NullPointerException("Name can't be null.")
        }

        if (name.nameType.scientific) {
            if (name.nameType.formula) {
                return constructHybridFormulaScientificName(name)
            }
            if (name.nameType.autonym) {
                return constructAutonymScientificName(name)
            }
            return constructScientificName(name)
        }

        if (name.nameType.cultivar) {
            if (name.nameType.formula && name.nameType.hybrid) {
                return constructHybridFormulaCultivarName(name)
            }
            if (name.nameType.formula) {
                return constructGraftChimeraName(name)
            }
            if (name.nameType.hybrid) {
                return constructHybridCultivarName(name)
            }
            return constructCultivarName(name)
        }

        if (name.nameType.name == 'informal') {
            return constructInformalName(name)
        }

        if (name.nameType.nameCategory?.name == 'common') {
            String htmlNameElement = name.nameElement.encodeAsHTML()
            String markedUpName = "<common><name data-id='$name.id'><element>${htmlNameElement}</element></name></common>"
            return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName]
        }

        return [fullMarkedUpName: (name.nameElement?.encodeAsHTML() ?: '?'), simpleMarkedUpName: (name.nameElement.encodeAsHTML() ?: '?')]
    }

    private Map constructInformalName(Name name) {
        List<String> bits = ["<element>${name.nameElement.encodeAsHTML()}</element>", constructAuthor(name)]

        String markedUpName = "<informal><name data-id='$name.id'>${join(bits)}</name></informal>"
        return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName]
    }

    private Map constructHybridCultivarName(Name name) {
        use(NameUtils) {
            List<String> bits = []
            String htmlNameElement = name.nameElement.encodeAsHTML()
            //NSL-856 cultivar hybrid display genus + epithet
            Name parent = name.nameParentOfRank('Genus')
            if (parent) {
                bits << constructName(parent).simpleMarkedUpName?.removeManuscript()
                bits << (name.nameType.connector) ? "<hybrid data-id='$name.nameType.id' title='$name.nameType.name'>$name.nameType.connector</hybrid>" : ''
                bits << "<element>&lsquo;${htmlNameElement}&rsquo;</element>"
            } else {
                bits << "<element>${htmlNameElement}</element>"
            }
            String markedUpName = "<cultivar><name data-id='$name.id'>${join(bits)}</name></cultivar>"
            return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName]
        }
    }

    private Map constructGraftChimeraName(Name name) {
        use(NameUtils) {
            List<String> bits = []
            Name parent = name.parent
            bits << constructName(parent).simpleMarkedUpName?.removeManuscript()
            bits << (name.nameType.connector ? "<formula data-id='$name.nameType.id' title='$name.nameType.name'>$name.nameType.connector</formula>" : '')
            bits << (name.secondParent ? constructName(name.secondParent).simpleMarkedUpName?.removeManuscript() : '')
            String markedUpName = "<cultivar><name data-id='$name.id'>${join(bits)}</name></cultivar>"
            return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName]
        }
    }

    private Map constructHybridFormulaCultivarName(Name name) {
        use(NameUtils) {
            List<String> bits = []
            Name parent = name.parent
            bits << constructName(parent).simpleMarkedUpName?.removeManuscript()
            bits << (name.nameType.connector ? "<hybrid data-id='$name.nameType.id' title='$name.nameType.name'>$name.nameType.connector</hybrid>" : '')
            bits << (name.secondParent ? constructName(name.secondParent).simpleMarkedUpName?.removeManuscript() : '')
            String markedUpName = "<cultivar><name data-id='$name.id'>${join(bits)}</name></cultivar>"
            return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName]
        }
    }

    private Map constructCultivarName(Name name) {
        use(NameUtils) {
            List<String> bits = []
            //NSL-927 cultivar display to lowest parent rank
            if (name.parent) {
                Map n = constructName(name.parent)
                bits << n.simpleMarkedUpName?.removeManuscript()
                bits << (name.nameType.connector ? "<hybrid data-id='$name.nameType.id' title='$name.nameType.name'>$name.nameType.connector</hybrid>" : '')
                bits << "<element>&lsquo;${name.nameElement.encodeAsHTML()}&rsquo;</element>"
            } else {
                bits << "'<element>${name.nameElement.encodeAsHTML()}</element>"
            }
            String markedUpName = "<cultivar><name data-id='$name.id'>${join(bits)}</name></cultivar>"
            return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName]
        }
    }


    private Map constructHybridFormulaScientificName(Name name) {
        use(NameUtils) {
            String firstParent = constructPrecedingNameString(name.parent, name)
            String connector = makeConnectorString(name, null)
            String secondParent = name.secondParent ? constructPrecedingNameString(name.secondParent, name) : '<element>?</element>'
            String manuscript = (name.nameStatus.name == 'manuscript') ? '<manuscript>MS</manuscript>' : ''

            List<String> simpleNameParts = [firstParent, connector, secondParent, manuscript]

            String markedUpName = "<scientific><name data-id='$name.id'>${join(simpleNameParts)}</name></scientific>"
            //need to remove Authors below from simple name because preceding name includes author in parent
            return [fullMarkedUpName: markedUpName, simpleMarkedUpName: markedUpName.removeAuthors()]
        }
    }

    private Map constructAutonymScientificName(Name name) {
        use(NameUtils) {
            Name nameParent = name.nameParent()
            String precedingName = constructPrecedingNameString(nameParent, name)
            String rank = nameParent ? makeRankString(name) : ''
            String connector = makeConnectorString(name, rank)
            String element = "<element>${name.nameElement.encodeAsHTML()}</element>"
            String manuscript = (name.nameStatus.name == 'manuscript') ? '<manuscript>MS</manuscript>' : ''

            List<String> simpleNameParts = [precedingName, rank, connector, element, manuscript]

            String fullMarkedUpName = "<scientific><name data-id='$name.id'>${join(simpleNameParts)}</name></scientific>"
            //need to remove Authors below from simple name because preceding name includes author in autonyms
            return [fullMarkedUpName: fullMarkedUpName, simpleMarkedUpName: fullMarkedUpName.removeAuthors()]
        }
    }

    private Map constructScientificName(Name name) {
        use(NameUtils) {
            Name nameParent = name.nameParent()
            String precedingName = constructPrecedingNameString(nameParent, name)

            String rank = nameParent ? makeRankString(name) : ''
            String connector = makeConnectorString(name, rank)
            String element = "<element>${name.nameElement.encodeAsHTML()}</element>"
            String author = constructAuthor(name)
            String manuscript = (name.nameStatus.name == 'manuscript') ? '<manuscript>MS</manuscript>' : ''

            List<String> fullNameParts = [precedingName, rank, connector, element, author, manuscript]
            List<String> simpleNameParts = [precedingName, rank, connector, element, manuscript]

            String fullMarkedUpName = "<scientific><name data-id='$name.id'>${join(fullNameParts)}</name></scientific>"
            String simpleMarkedUpName = "<scientific><name data-id='$name.id'>${join(simpleNameParts)}</name></scientific>"
            return [fullMarkedUpName: fullMarkedUpName, simpleMarkedUpName: simpleMarkedUpName]
        }
    }

    private String constructPrecedingNameString(Name parent, Name child) {
        use(NameUtils) {
            if (parent) {
                Map constructedName = constructName(parent)
                if (child.nameType.autonym) {
                    return constructedName.fullMarkedUpName.removeManuscript()
                }
                if (child.nameType.formula) {
                    if (parent.nameType.formula) {
                        return "(${constructedName.fullMarkedUpName.removeManuscript()})"
                    }
                    return constructedName.fullMarkedUpName.removeManuscript()
                }
                return constructedName.simpleMarkedUpName.removeManuscript()
            }
            return ''
        }
    }

    String makeConnectorString(Name name, String rank) {
        if (name.nameType.connector &&
                !(rank && name.nameType.connector == 'x' && name.nameRank.abbrev.startsWith('notho'))) {
            return "<hybrid data-id='$name.nameType.id' title='$name.nameType.name'>$name.nameType.connector</hybrid>"
        } else {
            return ''
        }
    }

    String makeRankString(Name name) {
        if (name.nameRank?.visibleInName) {
            if (name.nameRank.useVerbatimRank && name.verbatimRank) {
                return "<rank data-id='${name.nameRank?.id}'>${name.verbatimRank}</rank>"
            }
            return "<rank data-id='${name.nameRank?.id}'>${name.nameRank?.abbrev}</rank>"
        }
        return ''
    }

    String constructAuthor(Name name) {
        List<String> bits = []
        if (name.author) {
            if (name.baseAuthor) {
                if (name.exBaseAuthor) {
                    bits << "(<ex-base data-id='$name.exBaseAuthor.id' title='${name.exBaseAuthor.name.encodeAsHTML()}'>$name.exBaseAuthor.abbrev</ex-base> ex <base data-id='$name.baseAuthor.id' title='${name.baseAuthor.name.encodeAsHTML()}'>$name.baseAuthor.abbrev</base>)"
                } else {
                    bits << "(<base data-id='$name.baseAuthor.id' title='${name.baseAuthor.name.encodeAsHTML()}'>$name.baseAuthor.abbrev</base>)"
                }
            }
            if (name.exAuthor) {
                bits << "<ex data-id='$name.exAuthor.id' title='${name.exAuthor.name.encodeAsHTML()}'>$name.exAuthor.abbrev</ex> ex"
            }
            bits << "<author data-id='$name.author.id' title='${name.author.name.encodeAsHTML()}'>$name.author.abbrev</author>"
            if (name.sanctioningAuthor) {
                bits << ": <sanctioning data-id='$name.sanctioningAuthor.id' title='${name.sanctioningAuthor.name.encodeAsHTML()}'>$name.sanctioningAuthor.abbrev</sanctioning>"
            }
        }
        return bits.size() ? "<authors>${join(bits)}</authors>" : ''
    }
}

class NameUtils {

    static Name nameParent(Name name) {
        use(RankUtils) {
            if (name.nameRank.name == '[unranked]') {
                return firstMajorRankedParent(name)
            }
            if (name.nameLowerThanRank('Genus') || name.nameRank.visibleInName) {
                NameRank parentRank = name.nameRank.parentRank
                if (parentRank) {
                    int count = 5 //count to prevent recursive parents causing issues
                    while (count-- > 0 && name?.nameLowerThanRank(parentRank)) {
                        if (name.parent && name.parent.nameRank == parentRank) {
                            return name.parent
                        }
                        name = name.parent
                    }
                }
            }
            return null
        }
    }

    static Name firstMajorRankedParent(Name name) {
        int count = 5
        while (count-- > 0) {
            if (name.parent.nameRank.name != '[unranked]' && name.parent.nameRank.major) {
                return name.parent
            }
            name = name.parent
        }
        return null
    }

    static Name nameParentOfRank(Name name, String rankName) {
        Name parent = name.parent
        int count = 5
        while (count-- > 0) {
            if (parent.nameRank.name == rankName) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    static String removeManuscript(String string) {
        string.replaceAll(/ (<manuscript>MS<\/manuscript>)/, '')
    }

    static String removeAuthors(String string) {
        string.replaceAll(/ (<authors>.*?<\/authors>)/, '')
    }
}