export ScriptsRel=`basename "${0:a:h}"`
export TmuxInitDir=`dirname "${0:a:h}"`
export TmuxSessionName='Useless'

setopt IGNORE_EOF

source "${0:a:h}/.sbt-helpers.sh"
source "${0:a:h}/.tmux-helpers.sh"
