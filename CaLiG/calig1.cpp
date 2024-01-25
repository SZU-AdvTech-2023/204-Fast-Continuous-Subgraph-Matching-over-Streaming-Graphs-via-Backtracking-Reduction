#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <set>
#include <unordered_set>
#include <map>
#include <unordered_map>
#include <ctime>
#include <sstream>

using namespace std;
using u_set = unordered_set<int>;
using vec = vector<int>;


//查询子图结点信息
struct vertex_Q{
    int label;
    u_set nei;
    //key：Q[id]的nei.label
    //value：Q[id]的nei的id
    unordered_map<int, vec> rep_nei;
};

//数据图结点信息
struct vertex_G{
    int label;
    u_set nei;
    unordered_map<int, unordered_map<int, u_set>> cand;
    unordered_map<int, bool> LI;
};
int cnt = 0;

//查询图结点
vector<vertex_Q> Q;
//数据图结点
vector<vertex_G> G;
int Q_size, G_size;
vec update;
//key：label
//val：label为key的id
unordered_map<int, vec> labels;

//迭代判断是否在已有的匹配集合里
bool isInVec(int a, vec& v){
    for(int i=0; i<v.size(); i++){
        if(a == v[i]) return 1;
    }
    return 0;
}

//计算两个集合的交集
u_set intersection(u_set& us1, u_set& us2){
    u_set result;
    if(us1.size()<=us2.size()){
        for(auto& item : us1){
            if(us2.find(item)!=us2.end()) result.insert(item);
        }
    }
    else{
        for(auto& item : us2){
            if(us1.find(item)!=us1.end()) result.insert(item);
        }
    }
    return result;
}

void dumpG(const string& path){
    ofstream outfile(path);
    for(int vi=0; vi<G_size; vi++){
        for(auto& candi : G[vi].cand){
            int ui = candi.first;
            if(G[vi].LI[ui]){
                outfile << "v" << vi << "-u" << ui << " nei:";
                for(auto& nei : candi.second){
                    int uj = nei.first;
                    outfile << " [u" << uj << "]";
                    for(int vj : nei.second){
                        if(G[vj].LI[uj]) outfile << " v" << vj;
                    }
                }
                outfile << endl;
            }
        }
    }
    outfile.close();
}


//输入查询子图
void inputQ(string& qp){
    ifstream qg(qp);
    if(!qg) cerr << "Fail to open query file." << endl;

    char c;
    int id, id1, id2, lb, dvir;
    vertex_Q u;
    while(qg >> c){
        if(c == 'v'){
            //获取输入的结点信息（id， label）并存入Q
            qg >> id >> lb;
            if(lb == -1) lb = 0;
            u.label = lb;
            Q.push_back(u);
        }
        else{
            qg >> id1 >> id2;
            Q[id1].nei.insert(id2);
            Q[id2].nei.insert(id1);
            //break减少if-else判断时间
            break;
        }
    }
    while(qg >> c){
        //获取边信息更新Q中结点信息（nei）
        qg >> id1 >> id2;
        Q[id1].nei.insert(id2);
        Q[id2].nei.insert(id1);
    }
    qg.close();
    Q_size = Q.size();

    for(int ui=0; ui<Q_size; ui++){
        //key：label
        //val：label为key的id
        labels[Q[ui].label].emplace_back(ui);
        
        //key：Q[id]的nei.label
        //value：Q[id]的nei的id
        unordered_map<int, vec> rep_nei;
        for(auto& uni : Q[ui].nei){
            int& uni_label = Q[uni].label;
            rep_nei[uni_label].emplace_back(uni);
        }
        for(auto& repi : rep_nei){
            if(repi.second.size()>1){
                Q[ui].rep_nei[repi.first] = repi.second;
            }
        }
    }
}

struct split{
    vec core;
    vector<u_set> core_nei;
    //key:核心节点
    //value:邻居为核心结点的外壳结点
    unordered_map<int, u_set> c_s_nei;
    vec shell;
    vector<u_set> shell_nei;
};

//key:id
//value:nei匹配顺序
unordered_map<int, unordered_map<int, split>> matching_order;


// 将i的nei插入匹配集合，不包括ui,uj
void insertNei(u_set &temp, int i, int ui, int uj, split& uj_second){
    for(auto& nei : Q[i].nei){
        int n = nei;
        if(n!=ui && n!=uj && !isInVec(n, uj_second.core) && !isInVec(n, uj_second.shell)){
            temp.insert(n);
        }
    }
}

// 邻居是否都是核心集
bool neiAllCore(int i, split& uj_second, int ui, int uj){
    for(auto& nei : Q[i].nei){
        int n = nei;
        if(n!=ui && n!=uj && !isInVec(n, uj_second.core)){
            return false;
        }
    }
    return true;
}

//所有的边生成匹配顺序
void generateMO(){
    for(int ui=0; ui<Q_size; ui++) {
        //key: id
        //value: 匹配顺序
        unordered_map<int, split> ui_first;
        //迭代nei
        for (auto &uj : Q[ui].nei) {
            if (uj >= 0) {
                //存储uj的匹配顺序信息
                split uj_second;
                u_set temp;
                // 将节点ui和uj的邻居信息插入集合temp中
                insertNei(temp, ui, ui, uj, uj_second);
                insertNei(temp, uj, ui, uj, uj_second);
                while(!temp.empty()){
                    u_set temp2 = temp;
                    for(auto& ti : temp2){
                        /*if (ui == 0 && uj == 2) {
                            cerr << temp2.size() << endl;
                            cerr << ti << endl;
                            cerr << 1111 << endl;
                            cerr << neiAllCore(ti, uj_second, ui, uj) << endl;
                        }*/
                        // 判断ti邻居结点是否都为核心结点
                        if(neiAllCore(ti, uj_second, ui, uj)){
                            /*if (ui == 0 && uj == 2) {
                                cerr << 3333 << endl;
                            }*/
                            // 获取邻居信息
                            u_set nei_info = Q[ti].nei;
                            int v_core;
                            for(int & i : uj_second.core){
                                if(nei_info.count(i)){
                                    v_core = i;
                                }
                            }
                            // 将ti加入uj_second的shell中
                            uj_second.c_s_nei[v_core].insert(uj_second.shell.size());
                            uj_second.shell.push_back(ti);
                            uj_second.shell_nei.push_back(nei_info);
                            temp.erase(ti);
                        }
                    }
                    //在每次循环中，选择一个节点作为核心节点。
                    //选择规则是找到具有最多邻居节点的非 ui 和 uj 的节点，将其加入核心节点，并将其邻居信息加入 temp 中。
                    if(!temp.empty()){
                       /* if (ui == 0 && uj == 2) {
                            cerr << 2222 << endl;
                        }*/
                        int nei_num=0;
                        int core_v;
                        for(auto& t : temp){
                            // 获取节点t的邻居信息
                            u_set nei_noij = Q[t].nei;
                            nei_noij.erase(ui);
                            nei_noij.erase(uj);
                            if(nei_noij.size() > nei_num){
                                nei_num = nei_noij.size();
                                core_v = t;
                            }
                        }
                        // 将core_v加入uj_second的核心节点中
                        uj_second.core.push_back(core_v);
                        u_set nei_info;
                        for(auto& nei : Q[core_v].nei){
                            int n = nei;
                            if(n == ui || n == uj || isInVec(n, uj_second.core)){
                                nei_info.insert(nei);
                            }
                        }
                        // 将nei_info加入uj_second的核心邻居中
                        uj_second.core_nei.push_back(nei_info);
                        insertNei(temp, core_v, ui, uj, uj_second);
                        temp.erase(core_v);
                    }
                }
                ui_first[uj] = uj_second;

                /*cerr << "ui - uj:" << ui << " " << uj << endl;
                cerr << "core.size()" << uj_second.core.size() << endl;
                for (int i : uj_second.core) {
                    cerr << i << endl;
                }
                cerr << "shell.size()" << uj_second.shell.size() << endl;
                for (int i : uj_second.shell) {
                    cerr << i << endl;
                }*/

            }
        }
        matching_order[ui] = ui_first;
    }
}

//输入数据图
void inputG(string& gp){
    ifstream dg(gp);
    if(!dg) cerr << "Fail to open graph file." << endl;

    char c;
    int id, id1, id2, lb;
    vertex_G v;
    while(dg >> c){
        if(c == 'v'){
            //获取id，label信息
            dg >> id >> lb;
            if(labels.count(lb))  v.label = lb;
            else  v.label = -1;
            G.push_back(v);
        }
        else{
            G_size = G.size();
            dg >> id1 >> id2;
            if(id1<G_size && id2<G_size && G[id1].label!=-1 && G[id2].label!=-1){
                G[id1].nei.insert(id2);
                G[id2].nei.insert(id1);
            }
            //同查询子图
            break;
        }
    }
    while(dg >> c){
        dg >> id1 >> id2;
        if(id1<G_size && id2<G_size && G[id1].label!=-1 && G[id2].label!=-1){
            G[id1].nei.insert(id2);
            G[id2].nei.insert(id1);
        }
    }
    dg.close();
}


//构建候选
void constructCand(){
    for(int vi=0; vi<G_size; vi++){
        int& lb = G[vi].label;
        if(lb!=-1){
            //迭代相同label的子图结点
            for(auto& ui: labels[lb]){
                unordered_map<int, u_set> ui_cand;
                for(auto& vj : G[vi].nei){
                   
                    int vj_lb = G[vj].label;
                    for(auto& uj : Q[ui].nei){
                        //cerr << "vi:" << vi << " ui:" << ui << " vj:" << vj << " uj:" << uj << endl;
                        if(ui_cand.count(uj) == 0) {
                            ui_cand[uj] = u_set();  
                        }
                        if(uj>=0 && vj>=0 && Q[uj].label==vj_lb){
                            ui_cand[uj].insert(vj);
                        }
                    }
                }
                //更新vi关于ui的候选集合
                G[vi].cand[ui] = ui_cand;
                G[vi].LI[ui] = 1;
             /*   cerr << "vi " << vi << "ui:" << ui <<endl;
                for (const auto& entry : G[vi].cand[ui]) {
                    cerr << "uj: " << entry.first << "   vj: ";
                    for (int i : entry.second) {
                        cerr << i << " ";
                    }
                    cerr << endl;
                }*/
            }
        }
    }
    //dumpG("./dump/G_init");
}

//递归地匹配重复邻居集合中的每个节点，如果所有节点都能成功匹配，说明匹配成功
bool tryNei(int th, int vi, int ui, u_set& used, vec& to_check){
    if(th==to_check.size()){
        return 1;
    }
    else{
        auto& uj = to_check[th];
        for(auto& vj : G[vi].cand[ui][uj]){
            if(G[vj].label!=Q[uj].label) {
                G[vj].cand.erase(uj);
                G[vj].LI.erase(uj);
                continue;
            }
            if(used.find(vj)==used.end()){
                used.insert(vj);
                if(tryNei(th+1, vi, ui, used, to_check)) return 1;
                used.erase(vj);
            }
        }
        return 0;
    }
}

//检查图节点 vi 是否与查询节点 ui的邻居匹配 （包括普通邻居和重复邻居）
bool checkNei(int vi, int ui){
    for(auto& uj : Q[ui].nei){
        //没找到
        if(G[vi].cand[ui].find(uj)==G[vi].cand[ui].end()) return 0;
        //为空
        else if(!G[vi].cand[ui][uj].empty()) continue;
        else return 0;
    }
    for(auto& rep_nei : Q[ui].rep_nei){
        u_set used;
        //cerr << !tryNei(0, vi, ui, used, rep_nei.second) << endl;
        if(!tryNei(0, vi, ui, used, rep_nei.second)) return 0;
    }
    return 1;
}

// 递归检查是否需要删除节点对应的候选节点
void deleteAndCheck(int ui, int vi){
    for(auto& nei : G[vi].cand[ui]){
        int uj = nei.first;
        for(auto& vj : nei.second){
            //cerr << "judge vi:" << vi << " ui:" << ui << " vj:" << vj << " uj:" << uj << endl;
            if(G[vj].label!=Q[uj].label) {
                //cerr << 111 << endl;
                G[vj].cand.erase(uj);
                G[vj].LI.erase(uj);
                continue;
            }
            if(G[vj].LI[uj]) {
                //cerr << "del vi:" << vi << " ui:" << ui << " vj:" << vj << " uj:" << uj << endl;
                G[vj].cand[uj][ui].erase(vi);
                if (G[vj].cand[uj][ui].empty()) {
                    //cerr << 222 << endl;
                    //cerr << "vj:" << vj << " uj:" << uj << endl;
                    G[vj].LI[uj] = 0;
                    deleteAndCheck(uj, vj);
                }
                else{
                    /*cerr << 333 << endl;*/
                    auto lb = Q[ui].label;
                    if (Q[uj].rep_nei.find(lb)!=Q[uj].rep_nei.end()){
                        auto& rep_nei = Q[uj].rep_nei[lb];
                        u_set used;
                        if(!tryNei(0, vj, uj, used, rep_nei)){
                            //cerr << 444 << endl;
                            G[vj].LI[uj] = 0;
                            deleteAndCheck(uj, vj); 
                        }
                    }
                }
            }
        }
    }
}

//关闭某个图节点 vi 关于查询节点的部分候选集合
void turnOff(int& vi){
    for(auto& candi : G[vi].cand){
        auto& ui = candi.first;
        if(G[vi].label!=Q[ui].label) {
            //cerr << "aa" << endl;
            G[vi].cand.erase(ui);
            G[vi].LI.erase(ui);
            continue;
        }
        if (!G[vi].LI[ui]) {
            //cerr << "bb" << "vi:" << vi << " ui:" << ui <<  endl;
            continue;
        }
        //cerr << "vi:" << vi << " ui:" << ui << "   " <<checkNei(vi, ui) << endl;
        if (!checkNei(vi, ui) ){
            G[vi].LI[ui] = 0;
            deleteAndCheck(ui, vi);
        }
    }
}

//OFF传播
void turnOffProcess(int v1, int v2){
    G[v1].nei.erase(v2);
    G[v2].nei.erase(v1);
    for(auto& candi : G[v1].cand){
        int ui = candi.first;
        if(G[v1].label!=Q[ui].label){
            G[v1].cand.erase(ui);
            G[v1].LI.erase(ui);
            continue;
        }
        for(auto& ui_nei : candi.second){
            int uj = ui_nei.first;
            if(Q[uj].label==G[v2].label){
                G[v1].cand[ui][uj].erase(v2);
                G[v2].cand[uj][ui].erase(v1);
            }
        }
    }
    
    //cerr << endl; cerr << endl;
    //cerr << "turnOff1 start" << endl;
    turnOff(v1);
    //cerr << "turnOff1 over" << endl;
    //cerr << "turnOff2 start" << endl;
    turnOff(v2);
    //cerr << "turnOff2 over" << endl;
}

//向图中添加匹配，并检查是否需要继续传播
void addAndCheck(int ui, int vi, vec& temp_v, vec& temp_u){
    for(auto& nei : G[vi].cand[ui]){
        int uj = nei.first;
        for(auto& vj : nei.second){
            if(G[vj].label!=Q[uj].label) {
                G[vj].cand.erase(uj);
                G[vj].LI.erase(uj);
                continue;
            }
            G[vj].cand[uj][ui].insert(vi);
            if(!G[vj].LI[uj]){
                if(checkNei(vj, uj)){
                    G[vj].LI[uj] = 1;
                    addAndCheck(uj, vj, temp_v, temp_u);
                }
                else{
                    temp_v.emplace_back(vj);
                    temp_u.emplace_back(uj);
                }
            }
        }
    }
}

//ON传播
void turnOnProcess(int& v1, int& v2){
    vec temp_v, temp_u;
    G[v1].nei.insert(v2);
    G[v2].nei.insert(v1);
    for(auto& candi : G[v1].cand){
        int ui = candi.first;
        if(G[v1].label!=Q[ui].label){
            G[v1].cand.erase(ui);
            G[v1].LI.erase(ui);
            continue;
        }
        for(auto& ui_nei : candi.second){
            int uj = ui_nei.first;
            if(G[v2].label == Q[uj].label){
                G[v1].cand[ui][uj].insert(v2);
                if(G[v1].LI[ui]){
                    G[v2].cand[uj][ui].insert(v1);
                    addAndCheck(ui, v1, temp_v, temp_u);
                    for(int i=0; i<temp_v.size(); i++){
                        int& v = temp_v[i];
                        int& u = temp_u[i];
                        if(!G[v].LI[u]){
                            deleteAndCheck(u, v);
                        }
                    }
                    temp_v.clear();
                    temp_u.clear();
                }
                else if ( checkNei(v1, ui) ){
                    G[v1].LI[ui] = true;
                    addAndCheck(ui, v1, temp_v, temp_u);
                    for(int i=0; i<temp_v.size(); i++){
                        int& v = temp_v[i];
                        int& u = temp_u[i];
                        if(!G[v].LI[u]){
                            deleteAndCheck(u, v);
                        }
                    }
                    temp_v.clear();
                    temp_u.clear();
                }
            }
        }
    }


}

//在匹配过程中对候选集进行静态过滤
void staticFilter(){
    for(int vi=0; vi<G_size; vi++){
        turnOff(vi);
    }
    //dumpG("./dump/G_static");
}

void dumpMatch(unordered_map<int, int>& m){
    cerr << "matching results is：" ;
    for(int i=0; i<m.size(); i++){
        int v = m[i];
        cerr << " u" << i << "-v" << v;
    }
    cerr << endl;
}


bool shellCand(vector<u_set>& result, unordered_map<int, int>& m, const vec& s, const vector<u_set>& s_n, vec& used) {
    int s_size = s.size();
    result.resize(s_size);
    for (int i = 0; i < s_size; i++) {
        auto p_ui = s_n[i].begin();
        if (s_n[i].size() > 1) {
            auto ui_l = *p_ui;
            ++p_ui;
            auto ui = *p_ui;
            result[i] = intersection(G[m[ui_l]].cand[ui_l][s[i]], G[m[ui]].cand[ui][s[i]]);
            for (++p_ui; p_ui != s_n[i].end(); ++p_ui) {
                ui = *p_ui;
                result[i] = intersection(result[i], G[m[ui]].cand[ui][s[i]]);
            }
            if (result[i].empty()) return 1;
        }
        else {
            auto ui = *p_ui;
            result[i] = G[m[ui]].cand[ui][s[i]];
        }
        for (auto& vth : used) {
            result[i].erase(vth);
        }
        if (result[i].empty()) return 1;
    }
    return 0;
}

//在当前外壳节点集合下搜索到的匹配数量
int numAdd(int th, const vector<u_set>& cand, u_set& used){
    int result = 0;
    if(th==cand.size()-1){
        int del = 0;
        for(auto& vth : used){
            if(cand[th].find(vth)!=cand[th].end()){
                del += 1;
            }
        }
        return cand[th].size() - del;
    }
    for(auto& vth : cand[th]){
        if(used.find(vth)==used.end()){
            used.insert(vth);
            result += numAdd(th+1, cand, used);
            used.erase(vth);
        }
    }
    return result;
}

//检查一个外壳结点的邻居是否存在于候选集合中
bool notExit(int shell, u_set nei, unordered_map<int, int>& m){
    if(nei.size()==1){
        return false;
    }
    int n = *nei.begin();
    for (auto cand : G[m[n]].cand[n][shell]){
        int count = 0;
        for(int ni : nei){
            count += 1;
            if(count==1){
                continue;
            }
            else{
                if(G[m[ni]].cand[ni][shell].count(cand)){
                    if(count == nei.size()){
                        return false;
                    }
                }
                else{
                    break;
                }
            }
        }
    }
    return true;
}

//获取在当前核心节点下搜索到的匹配数量
int searchCore(int th, unordered_map<int, int>& m, vec& used, const vec& c, const vector<u_set>& c_n, const vec& s, const vector<u_set>& s_n, unordered_map<int, u_set>& c2check){
    int result = 0;
    //核心结点够，外壳结点搜索
    if(th == c.size()){
        vector<u_set> candidates;
        //存在任何集合为空的情况，说明匹配无法进行，返回0
        if(shellCand(candidates, m, s, s_n, used)) return 0;
        u_set used_v;
        return numAdd(0, candidates, used_v);
    }
    u_set candidates;
    auto p_ui = c_n[th].begin();
    auto ui = *p_ui;
    if(c_n[th].size()>1){
        u_set* temp;
        temp = &G[m[ui]].cand[ui][c[th]];
        candidates = intersection(*temp,G[m[ui]].cand[ui][c[th]]);
        for(++p_ui;p_ui!=c_n[th].end();++p_ui){
            ui = *p_ui;
            candidates = intersection(candidates,G[m[ui]].cand[ui][c[th]]);
            if(candidates.empty()) return 0;
        }
        for(auto& vi : used){
            candidates.erase(vi);
        }
        for(auto& vth : candidates){
            m[c[th]] = vth;
            if(c2check.count(c[th])){
                bool to_continue = false;
                for (auto& shell : c2check[c[th]]){
                    if(notExit(s[shell], s_n[shell], m)) {
                        to_continue = true;
                    }
                }
                if(to_continue) {
                    continue;
                }
            }
            used.emplace_back(vth);
            result += searchCore(th+1, m, used, c, c_n, s, s_n, c2check);
            used.pop_back();
        }
    }
    else{
        candidates = G[m[ui]].cand[ui][c[th]];
        for(auto& vth : candidates){
            if(!isInVec(vth, used)){
                m[c[th]] = vth;
                if(c2check.count(c[th])){
                    bool to_continue = false;
                    for (auto& shell : c2check[c[th]]){
                        if(notExit(s[shell], s_n[shell], m)) {
                            to_continue = true;
                        }
                    }
                    if(to_continue) {
                        continue;
                    }

                }
                used.emplace_back(vth);
                result += searchCore(th+1, m, used, c, c_n, s, s_n, c2check);
                used.pop_back();
            }
        }
    }
    return result;
}


//给定的两个节点之间搜索匹配，并返回更新的匹配数量
int searchMatch(int v1, int v2){
    int update_result = 0;
    for(auto& li : G[v1].LI){
        if(li.second){
            int u1 = li.first;
            for(auto& candi : G[v1].cand[u1]){
                int u2 = candi.first;
                if(u2>=0 && candi.second.find(v2)!=candi.second.end()){
                    cnt++;
                    //cerr << cnt << " v1:" << v1 << " u2:" << u2 << " v2:" << v2 << endl;
                    unordered_map<int, int> matching;
                    matching[u1] = v1;
                    matching[u2] = v2;
                    vec core_v;
                    core_v.emplace_back(v1);
                    core_v.emplace_back(v2);
                    
                    update_result += searchCore(0, matching, core_v, matching_order[u1][u2].core,
                                                matching_order[u1][u2].core_nei, matching_order[u1][u2].shell,
                                                matching_order[u1][u2].shell_nei, matching_order[u1][u2].c_s_nei);
                    
                }
            }
        }
    }
    return update_result;
}


// 获取增加删除边的更新信息
void inputUpdate(string& path, int max_num){
    update.clear();
    ifstream infile(path);
    char c;
    int v1, v2, w;
    int cnt = 0;
    while (infile >> c >> v1 >> v2){
        if(max_num!=0 && ++cnt>max_num) break;
        update.push_back(v1);
        update.push_back(v2);
    }
}

    //更新匹配过程
    void updateAndMatching(const std::vector<std::string>& path) {
        long long add_matches = 0;
        long long del_matches = 0;
        double update_time = 0.0;
        double search_time = 0.0;
        clock_t time;

        for (int t = 0; t < update.size(); t += 2) {
            int v1 = update[t];
            int v2 = update[t + 1];
            //cerr << v1 << "  " << v2 << endl;
            //负数输入表示删除 先搜索匹配，再OFF传播
            if(v1<0){
                if(G[-v1-1].label==-1 || G[-v2-1].label==-1) continue;
                if(!G[-v1-1].nei.count(-v2-1)) continue;
                time = clock();
                del_matches += searchMatch(-v1-1, -v2-1);
                //cerr << del_matches << endl;
                search_time += double(clock() - time)*1000 / CLOCKS_PER_SEC;
                time = clock();
                turnOffProcess(-v1-1, -v2-1);
                update_time += double(clock() - time) * 1000 / CLOCKS_PER_SEC;
                /*for (int vi = 0; vi < G_size; vi++) {
                   int& lb = G[vi].label;
                   if (lb != -1) {
                       for (auto& ui : labels[lb]) {
                         cerr << "vi " << vi << "ui:" << ui << "  " << G[vi].LI[ui] << endl;
                         for (const auto& entry : G[vi].cand[ui]) {
                             cerr << "uj: " << entry.first << "   vj: ";
                             for (int i : entry.second) {
                                 cerr << i << " ";
                             }
                             cerr << endl;
                         }
                       }
                   }
                }
                cerr << "sssssssssssssssssss" << endl;*/
            }
            //正数输入表示添加 先ON传播，再搜索匹配
            else{
                if(G[v1].label==-1 || G[v2].label==-1) continue;
                if(G[v1].nei.count(v2)) continue;
                time = clock();       
                turnOnProcess(v1, v2);
                update_time += double(clock() - time)*1000 / CLOCKS_PER_SEC;
                time = clock();
                add_matches += searchMatch(v1, v2);
                search_time += double(clock() - time)*1000 / CLOCKS_PER_SEC;
            }
        }
        
        cerr << "added matches: " << add_matches << endl;
        cerr << "deleted matches: " << del_matches << endl;
        cerr << "updated matches: " << add_matches + del_matches << endl;
        cerr << "update time: " << update_time << "ms" << endl;
        cerr << "search time: " << search_time << "ms" << endl;

      /*  for (const auto& element : path) {
            std::cout << element << std::endl;
        }
        cerr << endl;
        cerr << path[0] << endl;
        cerr << path[1] << endl;
        cerr << path[2] << endl;
        cerr << path[3] << endl;*/
        //string file_name = 
        //std::ofstream outputFile(path[1] + ".txt", std::ios::app);

        //// 检查文件是否成功打开
        //if (!outputFile.is_open()) {
        //    std::cerr << "Error opening output file." << std::endl;
        //}
        //
        //// 写入值到文件
        //outputFile << path[2] << path[3] << " " << path[4] << " updated matches: " << add_matches + del_matches << " backtrackings:" << cnt << endl;
        //// 关闭文件
        //outputFile.close();

    }

    //更新匹配过程
    void updateAndMatching() {
        long long add_matches = 0;
        long long del_matches = 0;
        double update_time = 0.0;
        double search_time = 0.0;
        clock_t time;

        for (int t = 0; t < update.size(); t += 2) {
            int v1 = update[t];
            int v2 = update[t + 1];
            //cerr << v1 << "  " << v2 << endl;
            //负数输入表示删除 先搜索匹配，再OFF传播
            if (v1 < 0) {
                if (G[-v1 - 1].label == -1 || G[-v2 - 1].label == -1) continue;
                if (!G[-v1 - 1].nei.count(-v2 - 1)) continue;
                time = clock();
                del_matches += searchMatch(-v1 - 1, -v2 - 1);
                //cerr << del_matches << endl;
                search_time += double(clock() - time) * 1000 / CLOCKS_PER_SEC;
                time = clock();
                turnOffProcess(-v1 - 1, -v2 - 1);
                update_time += double(clock() - time) * 1000 / CLOCKS_PER_SEC;
                /*for (int vi = 0; vi < G_size; vi++) {
                   int& lb = G[vi].label;
                   if (lb != -1) {
                       for (auto& ui : labels[lb]) {
                         cerr << "vi " << vi << "ui:" << ui << "  " << G[vi].LI[ui] << endl;
                         for (const auto& entry : G[vi].cand[ui]) {
                             cerr << "uj: " << entry.first << "   vj: ";
                             for (int i : entry.second) {
                                 cerr << i << " ";
                             }
                             cerr << endl;
                         }
                       }
                   }
                }
                cerr << "sssssssssssssssssss" << endl;*/
            }
            //正数输入表示添加 先ON传播，再搜索匹配
            else {
                if (G[v1].label == -1 || G[v2].label == -1) continue;
                if (G[v1].nei.count(v2)) continue;
                time = clock();
                turnOnProcess(v1, v2);
                update_time += double(clock() - time) * 1000 / CLOCKS_PER_SEC;
                time = clock();
                add_matches += searchMatch(v1, v2);
                search_time += double(clock() - time) * 1000 / CLOCKS_PER_SEC;
            }
        }
        cerr << "added matches: " << add_matches << endl;
        cerr << "deleted matches: " << del_matches << endl;
        cerr << "updated matches: " << add_matches + del_matches << endl;
        cerr << "update time: " << update_time << "ms" << endl;
        cerr << "search time: " << search_time << "ms" << endl;
    }

int main(int argc, char** argv){

    string g_path = "../dataset/dz_3/initial";
    string q_path = "../dataset/dz_3/Q/8/q10";
    string s_path = "../dataset/dz_3/s";
    int cnum = 0;
    for (int i=1; i<argc; i++) {
        if(string(argv[i]) == "-d")
            g_path = argv[i+1];
        else if(string(argv[i]) == "-q")
            q_path = argv[i+1];
        else if(string(argv[i]) == "-c")
            cnum = atoi(argv[i+1]);
        else if(string(argv[i]) == "-s") {
            s_path = argv[i+1];
        }
    }
    //std::string inputString = "./dataset/dz_3/initial";
    //char delimiter = '/';  // 分隔符是斜杠 '/'

   /* std::istringstream iss(q_path);
    std::string token;
    std::vector<std::string> tokens;

    while (std::getline(iss, token, delimiter)) {
        tokens.push_back(token);
    }*/
    /*for (const auto& element : tokens) {
        std::cout << element << std::endl;
    }
    cerr << tokens[0] << endl;*/



    clock_t time = clock()  ;
    cerr << "========== Start inputting. ==========" << endl;
    inputQ(q_path);
    generateMO();
    inputG(g_path);
    inputUpdate(s_path, cnum);
    cerr << "Inputting cost (ms): " << double(clock() - time)*1000 / CLOCKS_PER_SEC << endl;
    cerr << "The number of vertices in Q: " << Q_size << endl;
    cerr << "The number of vertices in G: " << G_size << endl;
    cerr << "========== End inputting. ==========" << endl;

    time = clock();
    cerr << "========== Start constructing. ==========" << endl;
    constructCand();
    cerr << "Constructing cost (ms): " << double(clock() - time)*1000 / CLOCKS_PER_SEC << endl;
    cerr << "========== End constructing. ==========" << endl;

    time = clock();
    cerr << "========== Start static filtering. ==========" << endl;
    staticFilter();
    cerr << "Static filtering cost (ms): " << double(clock() - time)*1000 / CLOCKS_PER_SEC << endl;
    cerr << "========== End static filtering. ==========" << endl;
    
   
    time = clock();
    cerr << "========== Start updating. ==========" << endl;
    //updateAndMatching(tokens);
    updateAndMatching();
    long long totally_cost = double(clock() - time) * 1000 / CLOCKS_PER_SEC;
    cerr << "Updating totally cost (ms): " << double(clock() - time)*1000 / CLOCKS_PER_SEC << endl;
    cerr << "========== End updating. ==========" << endl;

    //std::ofstream outputFile(tokens[1] + ".txt", std::ios::app);
    //// 检查文件是否成功打开
    //if (!outputFile.is_open()) {
    //    std::cerr << "Error opening output file." << std::endl;
    //}
    //// 写入值到文件
    //outputFile << "Updating totally cost(ms) : " << totally_cost << endl;
    //// 关闭文件
    //outputFile.close();
    
    //std::ofstream outFile("./output1.txt", std::ios::app);
    // // 检查文件是否成功打开
    //if (!outFile.is_open()) {
    //    std::cerr << "Error opening output file." << std::endl;
    //}
    //for (int vi = 0; vi < G_size; vi++) {
    //    int& lb = G[vi].label;
    //    if (lb != -1) {
    //        for (auto& ui : labels[lb]) {
    //            outFile << "vi " << vi << "ui:" << ui << "  " << G[vi].LI[ui] << endl;
    //            //cerr << "vi " << vi << "ui:" << ui << "  " << G[vi].LI[ui] << endl;
    //            for (const auto& entry : G[vi].cand[ui]) {
    //                outFile << "uj: " << entry.first << "   vj: ";
    //                //cerr << "uj: " << entry.first << "   vj: ";
    //                for (int i : entry.second) {
    //                    outFile << i << " ";
    //                    //cerr << i << " ";
    //                }
    //                outFile << endl;
    //                //cerr << endl;
    //            }
    //        }
    //    }
    //}
    // 关闭文件
    //outFile.close();

    cerr << "========== DONE!! ==========" << endl;
    cerr << "========== DONE!! ==========" << endl;
    cerr << cnt << endl;
    return 0;
    // 指标：设置超时时间（未完成查询） 
    // 平均运行时间 Updating totally cost (ms) 
    // 完成率 超时 
    // 匹配密度 将度量匹配密度定义为一次回溯生成的平均匹配数
}