import io

path = 'app/assets/css/main.css'
s = io.open(path, encoding='utf-8-sig').read()

anchor = '.message-comment-send:disabled { opacity: .5; cursor: default; }'
assert anchor in s, 'anchor not found'

css = """
.message-wall-head-right { display: flex; align-items: center; gap: 12px; }
.message-auth-btn { display: inline-flex; align-items: center; gap: 6px; padding: 5px 12px; border: 1px solid var(--line); border-radius: 999px; background: transparent; color: var(--muted); font-size: 13px; cursor: pointer; transition: all .15s ease; }
.message-auth-btn:hover, .message-auth-btn.open { color: var(--accent-dark); border-color: var(--accent); background: var(--soft-green); }
.message-user-chip { display: inline-flex; align-items: center; gap: 6px; padding: 4px 8px 4px 10px; border: 1px solid var(--accent); border-radius: 999px; background: var(--soft-green); color: var(--accent-dark); font-size: 13px; }
.message-user-logout { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; border: none; border-radius: 50%; background: transparent; color: var(--accent-dark); cursor: pointer; }
.message-user-logout:hover { background: rgba(0,0,0,0.06); }
.message-auth { margin-top: 16px; padding: 16px; border: 1px solid var(--line); border-radius: 12px; background: rgba(0,0,0,0.02); display: flex; flex-direction: column; gap: 10px; }
.message-auth-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 700; color: var(--ink); }
.message-auth-tip { margin: 0; font-size: 12.5px; color: var(--muted); line-height: 1.6; }
.message-auth input { width: 100%; box-sizing: border-box; padding: 9px 12px; border: 1px solid var(--line); border-radius: 8px; background: var(--card); color: var(--ink); font-size: 13px; }
.message-auth input:disabled { opacity: .6; }
.message-auth-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.message-auth-row input { flex: 1; min-width: 0; }
.message-auth-btn-small { flex: 0 0 auto; padding: 9px 14px; border: 1px solid var(--accent); border-radius: 8px; background: var(--soft-green); color: var(--accent-dark); font-size: 13px; cursor: pointer; }
.message-auth-btn-small:disabled { opacity: .5; cursor: default; }
.message-auth-sent { flex: 0 0 auto; font-size: 12.5px; color: var(--accent-dark); }
.message-auth-resend { font-size: 12.5px; color: var(--accent-dark); cursor: pointer; text-decoration: underline; }
.message-verified { padding: 1px 7px; border-radius: 999px; background: rgba(139,200,234,0.16); border: 1px solid #8BC8EA; color: #3f7ea8; font-size: 10px; font-weight: 700; }
.message-comment-as { margin: 0; font-size: 12.5px; color: var(--muted); }
.message-comment-as strong { color: var(--accent-dark); }
"""

s = s.replace(anchor, anchor + '\n' + css.strip('\n'))
crlf = s.replace('\n', '\r\n')
with io.open(path, 'w', encoding='utf-8', newline='') as f:
    f.write('\ufeff' + crlf)
print('inserted ok, new length:', len(crlf))
