#!/usr/bin/env node
/**
 * Wave21-A: 简易 fixture 静态服务器。
 * 用于 lighthouse-ci autorun startServerCommand，以及 axe-scan.mjs 的本地回退测试。
 * 不依赖前端 dist——读 tests/a11y/fixtures/*.html 内联响应，避免 ruoyi-web 构建未就绪时 CI 失败。
 *
 * 端口：A11Y_FIXTURE_PORT（默认 4173，与 ZK-IPD LIVE URL 一致，便于复用环境变量）
 * 路由：fixtures/<name>.html 或路径 → 内置 5 页最小 a11y 演示 HTML
 */
import http from 'node:http';
import path from 'node:path';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FIXTURE_DIR = path.join(__dirname, 'fixtures');
const PORT = Number(process.env.A11Y_FIXTURE_PORT || 4173);

// 内联最小 a11y 演示 HTML——故意保留少量 violations 便于 CI 验证流程跑通
const FIXTURES = {
  '/login': `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>登录</title></head>
<body><header><h1>IPD 产品经理系统</h1></header>
<main><h2>登录</h2>
<form action="/api/login" method="post">
  <label>用户名 <input name="username" autocomplete="username"></label>
  <label>密码 <input type="password" name="password" autocomplete="current-password"></label>
  <button type="submit">登录</button>
</form>
</main></body></html>`,
  '/workspace': `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>工作台</title></head>
<body><a href="#main">跳到主内容</a>
<header><h1>工作台</h1><nav aria-label="主导航"><ul><li><a href="/project-list">项目</a></li><li><a href="/gate-review">Gate 评审</a></li></ul></nav></header>
<main id="main"><h2>今日任务</h2>
<table><caption>待办</caption><thead><tr><th scope="col">任务</th><th scope="col">状态</th></tr></thead>
<tbody><tr><td>P-001 立项</td><td>进行中</td></tr></tbody></table>
</main></body></html>`,
  '/project-list': `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>项目列表</title></head>
<body><main><h1>项目列表</h1>
<table><caption>项目</caption><thead><tr><th scope="col">名称</th><th scope="col">阶段</th></tr></thead>
<tbody><tr><td>智能客服 v2</td><td>概念</td></tr><tr><td>CRM 迁移</td><td>计划</td></tr></tbody></table>
</main></body></html>`,
  '/gate-review': `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>Gate 评审</title></head>
<body><main><h1>Gate 评审</h1>
<section aria-labelledby="g1"><h2 id="g1">Gate 1 - 概念决策</h2>
<form><fieldset><legend>评审要素</legend>
<label><input type="checkbox" name="m1">市场需求</label>
<label><input type="checkbox" name="m2">技术可行性</label>
<button type="submit">提交</button>
</fieldset></form>
</section></main></body></html>`,
  '/dashboard': `<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>看板</title></head>
<body><main><h1>看板</h1>
<section><h2>KPI</h2>
<p>本月完成 Gate 评审 5 项</p>
</section></main></body></html>`
};

const server = http.createServer((req, res) => {
  const url = (req.url || '/').split('?')[0];
  // 1) fixtures 静态文件
  if (url.startsWith('/fixtures/')) {
    const filePath = path.join(FIXTURE_DIR, url.replace('/fixtures/', ''));
    if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      fs.createReadStream(filePath).pipe(res);
      return;
    }
    res.writeHead(404); res.end('not found'); return;
  }
  // 2) 内联 fixture
  if (FIXTURES[url]) {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(FIXTURES[url]);
    return;
  }
  // 3) 默认 → /login
  res.writeHead(302, { Location: '/login' });
  res.end();
});

server.listen(PORT, () => {
  process.stdout.write(`fixture-server-ready port=${PORT}\n`);
});
