/* 课程导航：在 lessons/ 和 reference/ 页面动态注入侧边栏目录树 + 上下节导航 */
(function () {
  'use strict';

  // ========== 目录数据 ==========
  var NAV = [
    { group: "基础入门", items: [
      {n:"0001", title:"从环境检查到最小 ChatBot", file:"0001-from-environment-check-to-chatbot.html", ref:"0001-chatclient-quick-reference.html"},
      {n:"0002", title:"流式 ChatBot 与 SSE",      file:"0002-streaming-chatbot-sse.html",     ref:"0002-streaming-chatbot-sse-quick-reference.html"},
      {n:"0003", title:"Prompt 学习助手",           file:"0003-prompt-assistant.html",          ref:"0003-prompt-assistant-quick-reference.html"},
    ]},
    { group: "Prompt 工程", items: [
      {n:"0004", title:"结构化 JSON 输出",         file:"0004-structured-output.html",         ref:"0004-structured-output-quick-reference.html"},
      {n:"0005", title:"Prompt 模板管理",          file:"0005-prompt-template.html",          ref:"0005-prompt-template-quick-reference.html"},
      {n:"0006", title:"多轮对话 Memory",          file:"0006-chat-memory.html",              ref:"0006-chat-memory-quick-reference.html"},
    ]},
    { group: "记忆", items: [
      {n:"0007", title:"学习顾问 Memory",          file:"0007-learning-advisor.html",         ref:"0007-learning-advisor-quick-reference.html"},
      {n:"0008", title:"Tool Calling 入门：计算器", file:"0008-calculator-tool.html",         ref:"0008-calculator-tool-quick-reference.html"},
    ]},
    { group: "Tool Calling", items: [
      {n:"0009", title:"天气工具：外部 API 风格",   file:"0009-weather-tool.html",             ref:"0009-weather-tool-quick-reference.html"},
      {n:"0010", title:"订单查询 Agent",           file:"0010-order-agent.html",              ref:"0010-order-agent-quick-reference.html"},
      {n:"0011", title:"Markdown 知识库 RAG",      file:"0011-markdown-rag.html",             ref:"0011-markdown-rag-quick-reference.html"},
    ]},
    { group: "RAG 与综合 Agent", items: [
      {n:"0012", title:"PDF 知识库 RAG",           file:"0012-pdf-rag.html",                  ref:"0012-pdf-rag-quick-reference.html"},
      {n:"0013", title:"RAG 参数调优",             file:"0013-rag-tuning.html",               ref:"0013-rag-tuning-quick-reference.html"},
      {n:"0014", title:"智能客服 Agent 综合",       file:"0014-customer-service-agent.html",   ref:"0014-customer-service-agent-quick-reference.html"},
      {n:"0015", title:"Graph 客服 Agent",         file:"0015-graph-agent.html",              ref:"0015-graph-agent-quick-reference.html"},
    ]},
    { group: "进阶：协议、观测、平台", items: [
      {n:"0016", title:"MCP 工具集成",             file:"0016-mcp.html",                      ref:"0016-mcp-quick-reference.html"},
      {n:"0017", title:"Agent 日志与评测",         file:"0017-observability-eval.html",       ref:"0017-observability-eval-quick-reference.html"},
      {n:"0018", title:"企业级 Agent 平台原型",    file:"0018-agent-platform.html",           ref:"0018-agent-platform-quick-reference.html"},
    ]},
  ];

  // ========== 判断当前页 ==========
  var path = location.pathname;
  var parts = path.split('/');
  var fname = parts[parts.length-1];
  var dir = parts[parts.length-2]; // lessons 或 reference
  var isLesson = dir === 'lessons';
  var isRef    = dir === 'reference';
  if (!isLesson && !isRef) return; // index 页或其他页不注入

  // 所有 lesson 文件按编号排序
  var lessonFiles = [];
  var refFiles = [];
  NAV.forEach(function(g){ g.items.forEach(function(it){ lessonFiles.push(it.file); refFiles.push(it.ref); }); });
  var list = isLesson ? lessonFiles : refFiles;
  var idx = list.indexOf(fname);

  // ========== 渲染侧边栏 ==========
  var STORAGE_KEY = 'saa-nav-open';
  var savedOpen = null;
  try { savedOpen = localStorage.getItem(STORAGE_KEY); } catch(e){}
  var openByDefault = savedOpen === 'true';

  var mask = document.createElement('div');
  mask.id = 'saa-nav-mask';

  var sidebar = document.createElement('aside');
  sidebar.id = 'saa-nav-sidebar';
  if (openByDefault) sidebar.classList.add('open');

  var treeHtml = '';
  NAV.forEach(function(g, gi){
    var opened = openByDefault ? ' open' : '';
    treeHtml += '<details' + opened + '>';
    treeHtml += '<summary>' + escapeHtml(g.group) + '</summary>';
    treeHtml += '<ul>';
    g.items.forEach(function(it){
      var cls = '';
      var targetHref, refHref;
      if (isLesson) {
        targetHref = it.file;
        refHref = '../reference/' + it.ref;
        if (it.file === fname) cls = ' class="active"';
      } else {
        targetHref = '../reference/' + it.ref;
        refHref = '../lessons/' + it.file;
        if (it.ref === fname) cls = ' class="active"';
      }
      treeHtml += '<li><a href="' + targetHref + '"' + cls + '>';
      treeHtml += '<span class="num">' + it.n + '</span>';
      treeHtml += '<span class="title">' + escapeHtml(it.title) + '</span>';
      treeHtml += '<span class="ref-link" title="查看' + (isLesson?'速查卡':'课程')+'" onclick="event.stopPropagation()">' + (isLesson?'卡':'课') + '</span>';
      treeHtml += '</a></li>';
      // Bind ref link after DOM insert instead of inline: we'll do via delegated handler
      void refHref;
    });
    treeHtml += '</ul></details>';
  });

  sidebar.innerHTML =
    '<div class="saa-nav-header">' +
      '<a class="brand" href="../index.html">📚 Spring AI Alibaba 课程</a>' +
      '<button class="saa-nav-close" title="收起目录" aria-label="收起目录">×</button>' +
    '</div>' +
    '<div class="saa-nav-tree">' + treeHtml + '</div>' +
    '<div class="saa-nav-footer">' +
      '<a href="../index.html">← 课程首页</a>' +
      '<span>' + (isLesson ? '课程' : '速查卡') + '</span>' +
    '</div>';

  var toggle = document.createElement('button');
  toggle.id = 'saa-nav-toggle';
  toggle.title = '打开课程目录';
  toggle.setAttribute('aria-label', '打开课程目录');
  toggle.textContent = '≡';

  // 注入到 body
  function inject() {
    document.body.appendChild(mask);
    document.body.appendChild(sidebar);
    document.body.appendChild(toggle);
    if (openByDefault) document.body.classList.add('saa-nav-open'), mask.classList.add('show');
    bindRefLinks();
  }

  // 侧栏里的"卡/课"小链接需要跳转到正确的 reference/lesson 路径（单独绑定，因为 a 中嵌套 span 用事件委托）
  function bindRefLinks() {
    var links = sidebar.querySelectorAll('.saa-nav-tree li a');
    links.forEach(function(a, i){
      // find the corresponding item by n
      var numEl = a.querySelector('.num');
      if (!numEl) return;
      var n = numEl.textContent;
      var item = null;
      NAV.forEach(function(g){ g.items.forEach(function(it){ if (it.n===n) item=it; }); });
      if (!item) return;
      var refSpan = a.querySelector('.ref-link');
      if (!refSpan) return;
      // Convert the span into a proper link
      var href;
      if (isLesson) href = '../reference/' + item.ref;
      else href = '../lessons/' + item.file;
      refSpan.outerHTML = '<a class="ref-link" href="' + href + '" title="' + (isLesson?'查看速查卡':'查看课程') + '">' + (isLesson?'卡':'课') + '</a>';
    });
  }

  function openSidebar() {
    sidebar.classList.add('open');
    mask.classList.add('show');
    document.body.classList.add('saa-nav-open');
    try { localStorage.setItem(STORAGE_KEY, 'true'); } catch(e){}
  }
  function closeSidebar() {
    sidebar.classList.remove('open');
    mask.classList.remove('show');
    document.body.classList.remove('saa-nav-open');
    try { localStorage.setItem(STORAGE_KEY, 'false'); } catch(e){}
  }

  toggle.addEventListener('click', function(){
    if (sidebar.classList.contains('open')) closeSidebar(); else openSidebar();
  });
  mask.addEventListener('click', closeSidebar);
  sidebar.querySelector('.saa-nav-close').addEventListener('click', closeSidebar);
  // 点击侧栏内链接自动收起（移动端体验更好）
  sidebar.addEventListener('click', function(e){
    var a = e.target.closest('a');
    if (a && a.classList.contains('ref-link')) return; // ref 链接不收起，方便连续看
    if (a && a.href && !a.classList.contains('brand')) {
      closeSidebar();
    }
  });

  // ========== 底部上一节/下一节 ==========
  var pn = document.createElement('nav');
  pn.id = 'saa-nav-prev-next';
  var html = '';
  if (idx > 0) {
    html += '<a class="saa-nav-prev" href="' + list[idx-1] + '"><span class="saa-nav-dir">← 上一节</span><span class="saa-nav-title">' + (idx>0 ? NAV_itemTitle(idx-1) : '') + '</span></a>';
  } else {
    html += '<a class="saa-nav-prev disabled"><span class="saa-nav-dir">← 上一节</span><span class="saa-nav-title">已经是第一节</span></a>';
  }
  html += '<a class="saa-nav-home" href="../index.html" title="课程目录">🏠</a>';
  if (idx < list.length-1 && idx >= 0) {
    html += '<a class="saa-nav-next" href="' + list[idx+1] + '"><span class="saa-nav-dir">下一节 →</span><span class="saa-nav-title">' + NAV_itemTitle(idx+1) + '</span></a>';
  } else {
    html += '<a class="saa-nav-next disabled"><span class="saa-nav-dir">下一节 →</span><span class="saa-nav-title">已经学完 🎉</span></a>';
  }
  pn.innerHTML = html;

  function NAV_itemTitle(i) {
    var file = list[i];
    for (var g=0; g<NAV.length; g++) for (var j=0; j<NAV[g].items.length; j++) {
      var it = NAV[g].items[j];
      var f = isLesson ? it.file : it.ref;
      if (f === file) return escapeHtml(it.title);
    }
    return file;
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"]/g, function(c){
      return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];
    });
  }

  // 等 DOM 就绪
  if (document.body) inject();
  else document.addEventListener('DOMContentLoaded', inject);

  // 把上下节导航插到 <main> 末尾（main 已存在；defer 脚本执行时 DOM 已解析完）
  function injectPn() {
    var main = document.querySelector('main');
    if (main) main.appendChild(pn);
    else document.body.appendChild(pn);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', injectPn);
  else injectPn();
})();
