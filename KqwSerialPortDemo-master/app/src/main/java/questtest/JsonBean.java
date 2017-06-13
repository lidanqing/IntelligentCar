package questtest;

import java.util.List;

/**
 * Created by ${kang} on 2016/6/20.
 */

public class JsonBean {
    public int protocloId;
    public int result;
    public long sendTime;
    public int answerTypeId;
    public SingleNode singleNode;
    public VagueNode vagueNode;

    public int getResult(){
        return result;
    }
    public int getProtocloId(){
        return protocloId;
    }
    public long getSendTime(){
        return sendTime;
    }
    public int getAnswerTypeId(){
        return answerTypeId;
    }
    public SingleNode getSingleNode(){
        return singleNode;
    }
    public VagueNode getVagueNode(){
        return vagueNode;
    }

    public static class SingleNode {
        public long standardQuestionId;
        public int isRichText;
        public String answerMsg;
        public List<NewsNode> list;
        public double score;

        public long getStandardQuestionId(){
            return standardQuestionId;
        }
        public int getIsRichText(){
            return isRichText;
        }
        public String getAnswerMsg(){
            return answerMsg;
        }
        public double getScore(){
            return score;
        }
        public List<NewsNode> getList(){
            return list;
        }
    }
    public static class NewsNode {
        public int itemld;
        public String title;
        public String description;
        public String picUrl;
        public String url;
        public String cmd;

        public int getItemld(){
            return itemld;
        }
        public String getTitle(){
            return title;
        }
        public String getDescription(){
            return description;
        }
        public String getPicUrl(){
            return picUrl;
        }
        public String getUrl(){
            return url;
        }
        public String getCmd(){
            return cmd;
        }
    }
    public static class VagueNode{

        public List<String> questionList;
        public String promptVagueMsg;
        public String endVagueMsg;
        public List<ItemMsg> itemList;

        public List<String> getQuestionList(){
            return questionList;
        }
        public String getPromptVagueMsg(){
            return promptVagueMsg;
        }
        public String getEndVagueMsg(){
            return endVagueMsg;
        }
        public List<ItemMsg> getItemList(){
            return itemList;
        }
    }
    public static class ItemMsg {
        public String question;
        public double score;
        public int num;
        public long id;
        public int type;

        public String getQuestion(){
            return question;
        }
        public double getScore(){
            return score;
        }
        public int getNum(){
            return num;
        }
        public long getId(){
            return id;
        }
        public int getType(){
            return type;
        }

    }
}
