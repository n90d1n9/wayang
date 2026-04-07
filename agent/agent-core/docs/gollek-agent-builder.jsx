import { useState, useCallback } from "react";

const SKILLS = [
  { id: "inference",      name: "LLM Inference",     category: "REASONING",   description: "Execute prompts through configured LLM providers", color: "purple",  icon: "⬡" },
  { id: "rag",            name: "RAG",                category: "RETRIEVAL",   description: "Retrieval-augmented generation with vector store",  color: "teal",    icon: "⬡" },
  { id: "code-execution", name: "Code Executor",      category: "EXECUTION",   description: "Sandboxed code execution (Python/Java/JS)",          color: "amber",   icon: "⬡" },
  { id: "web-search",     name: "Web Search",         category: "RETRIEVAL",   description: "Live web search and content extraction",             color: "blue",    icon: "⬡" },
  { id: "data-analysis",  name: "Data Analysis",      category: "ANALYTICS",   description: "Tabular data analysis and transformation",           color: "green",   icon: "⬡" },
  { id: "summarization",  name: "Summarization",      category: "REASONING",   description: "Multi-document summarization",                       color: "coral",   icon: "⬡" },
  { id: "embedding",      name: "Embedding",          category: "VECTOR",      description: "Generate vector embeddings via local models",        color: "pink",    icon: "⬡" },
  { id: "memory-store",   name: "Memory Store",       category: "MEMORY",      description: "Store / retrieve from long-term agent memory",       color: "gray",    icon: "⬡" },
  { id: "http-call",      name: "HTTP Call",          category: "INTEGRATION", description: "Authenticated external HTTP API calls",              color: "teal",    icon: "⬡" },
  { id: "sql-query",      name: "SQL Query",          category: "DATA",        description: "Execute SQL against configured datasources",         color: "amber",   icon: "⬡" },
  { id: "document-qa",    name: "Document QA",        category: "RETRIEVAL",   description: "QA over uploaded documents",                         color: "purple",  icon: "⬡" },
  { id: "model-convert",  name: "Model Converter",    category: "TOOLING",     description: "Convert model formats (SafeTensors→GGUF etc.)",      color: "coral",   icon: "⬡" },
];

const STRATEGIES = [
  { id: "react",             name: "ReAct",             desc: "Reason → Act → Observe loop" },
  { id: "plan-and-execute",  name: "Plan & Execute",    desc: "Plan first, then execute steps" },
  { id: "cot",               name: "Chain of Thought",  desc: "Pure reasoning, no tools" },
  { id: "reflexion",         name: "Reflexion",         desc: "ReAct + self-reflection" },
  { id: "tree-of-thought",   name: "Tree of Thought",   desc: "Branching reasoning" },
];

const PALETTES = {
  purple: { bg: "#EEEDFE", border: "#7F77DD", text: "#3C3489" },
  teal:   { bg: "#E1F5EE", border: "#1D9E75", text: "#085041" },
  amber:  { bg: "#FAEEDA", border: "#BA7517", text: "#633806" },
  blue:   { bg: "#E6F1FB", border: "#378ADD", text: "#0C447C" },
  green:  { bg: "#EAF3DE", border: "#639922", text: "#27500A" },
  coral:  { bg: "#FAECE7", border: "#D85A30", text: "#4A1B0C" },
  pink:   { bg: "#FBEAF0", border: "#D4537E", text: "#4B1528" },
  gray:   { bg: "#F1EFE8", border: "#888780", text: "#2C2C2A" },
};

const SAMPLE_PROMPTS = [
  "Analyze the uploaded CSV and summarize top 5 customers by revenue",
  "Search recent AI papers and write a brief on key findings",
  "Answer questions about our product docs using the knowledge base",
  "Generate a Python script to clean and transform the sales dataset",
  "Monitor API health and alert if error rate exceeds 5%",
];

export default function GollekAgentBuilder() {
  const [tab, setTab] = useState("builder");
  const [strategy, setStrategy] = useState("react");
  const [selectedSkills, setSelectedSkills] = useState(["inference", "rag"]);
  const [prompt, setPrompt] = useState("");
  const [maxSteps, setMaxSteps] = useState(10);
  const [tenantId, setTenantId] = useState("community");
  const [timeout, setTimeout] = useState(60);
  const [modelId, setModelId] = useState("Qwen/Qwen2.5-7B-Instruct");
  const [streaming, setStreaming] = useState(false);
  const [verbose, setVerbose] = useState(false);
  const [catFilter, setCatFilter] = useState("ALL");
  const [preview, setPreview] = useState(false);

  const toggleSkill = useCallback((id) => {
    setSelectedSkills(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);
  }, []);

  const categories = ["ALL", ...Array.from(new Set(SKILLS.map(s => s.category)))];
  const filteredSkills = catFilter === "ALL" ? SKILLS : SKILLS.filter(s => s.category === catFilter);
  const selectedSkillData = SKILLS.filter(s => selectedSkills.includes(s.id));

  const generateCode = () => {
    const skillList = selectedSkills.map(s => `            .skill("${s}")`).join("\n");
    return `@Inject AgentBuilder agentBuilder;

// Execute agent task
Uni<AgentResponse> result = agentBuilder
    .newAgent()
    .withPrompt("${prompt || "Your prompt here..."}")
    .usingStrategy(AgentRequest.OrchestrationStrategy.${STRATEGIES.find(s=>s.id===strategy)?.name.replace(/[^A-Z]/g,"_").toUpperCase() || "REACT"})
${skillList}
    .withMaxSteps(${maxSteps})
    .withTimeout(Duration.ofSeconds(${timeout}))
    .forTenant("${tenantId}")
    .withModel("${modelId}")${streaming ? "\n    .streaming()" : ""}${verbose ? "\n    .verbose()" : ""}
    .execute();`;
  };

  const generateCurl = () => {
    return `curl -X POST http://localhost:8080/api/v1/agents/run \\
  -H "Content-Type: application/json" \\
  -H "X-Tenant-ID: ${tenantId}" \\
  -d '{
    "prompt": "${prompt || "Your prompt here..."}",
    "strategy": "${strategy}",
    "skills": ${JSON.stringify(selectedSkills)},
    "maxSteps": ${maxSteps},
    "timeout": "PT${timeout}S",
    "tenantId": "${tenantId}",
    "modelId": "${modelId}"
  }'`;
  };

  const generateYaml = () => {
    return `gollek:
  agent:
    enabled: true
    default-orchestrator: ${strategy}
    max-steps: ${maxSteps}
    timeout: PT${timeout}S
    
    skills:
      auto-discover: true
      allowed: ${JSON.stringify(selectedSkills)}
    
    tenants:
      ${tenantId}:
        max-concurrent-agents: 10
        max-steps: ${maxSteps}
    
    model:
      default-model: "${modelId}"${streaming ? "\n    streaming: true" : ""}`;
  };

  const [codeTab, setCodeTab] = useState("java");

  return (
    <div style={{fontFamily:"var(--font-sans)",color:"var(--color-text-primary)",maxWidth:900,margin:"0 auto",padding:"1.5rem 1rem"}}>
      {/* Header */}
      <div style={{marginBottom:"1.5rem"}}>
        <div style={{display:"flex",alignItems:"center",gap:12,marginBottom:6}}>
          <div style={{width:36,height:36,background:"#EEEDFE",border:"0.5px solid #7F77DD",borderRadius:8,display:"flex",alignItems:"center",justifyContent:"center",fontSize:18}}>⬡</div>
          <div>
            <h1 style={{fontSize:18,fontWeight:500,margin:0,letterSpacing:"-0.01em"}}>Gollek Agent Builder</h1>
            <p style={{fontSize:12,color:"var(--color-text-secondary)",margin:0}}>Compose, configure, and export agentic workflows</p>
          </div>
        </div>
        <div style={{display:"flex",gap:0,borderBottom:"0.5px solid var(--color-border-tertiary)",marginTop:"1rem"}}>
          {["builder","skills","code","architecture"].map(t=>(
            <button key={t} onClick={()=>setTab(t)} style={{
              padding:"8px 16px",fontSize:13,fontWeight:tab===t?500:400,
              background:"transparent",border:"none",borderBottom:tab===t?"2px solid var(--color-text-primary)":"2px solid transparent",
              color:tab===t?"var(--color-text-primary)":"var(--color-text-secondary)",cursor:"pointer",
              textTransform:"capitalize",transition:"color .15s"
            }}>{t}</button>
          ))}
        </div>
      </div>

      {/* BUILDER TAB */}
      {tab === "builder" && (
        <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:16}}>
          {/* Left col */}
          <div style={{display:"flex",flexDirection:"column",gap:12}}>
            {/* Prompt */}
            <div style={{background:"var(--color-background-primary)",border:"0.5px solid var(--color-border-tertiary)",borderRadius:10,padding:"1rem"}}>
              <label style={{fontSize:12,color:"var(--color-text-secondary)",display:"block",marginBottom:6,fontWeight:500}}>Agent prompt</label>
              <textarea
                value={prompt} onChange={e=>setPrompt(e.target.value)}
                placeholder="Describe the task for your agent..."
                style={{width:"100%",minHeight:80,resize:"vertical",border:"0.5px solid var(--color-border-tertiary)",borderRadius:6,padding:"8px 10px",fontSize:13,background:"var(--color-background-secondary)",color:"var(--color-text-primary)",boxSizing:"border-box",fontFamily:"var(--font-sans)"}}
              />
              <div style={{display:"flex",flexWrap:"wrap",gap:4,marginTop:8}}>
                {SAMPLE_PROMPTS.slice(0,3).map((p,i)=>(
                  <button key={i} onClick={()=>setPrompt(p)} style={{
                    fontSize:11,padding:"3px 8px",borderRadius:4,border:"0.5px solid var(--color-border-tertiary)",
                    background:"var(--color-background-secondary)",color:"var(--color-text-secondary)",cursor:"pointer"
                  }}>example {i+1}</button>
                ))}
              </div>
            </div>

            {/* Strategy */}
            <div style={{background:"var(--color-background-primary)",border:"0.5px solid var(--color-border-tertiary)",borderRadius:10,padding:"1rem"}}>
              <label style={{fontSize:12,color:"var(--color-text-secondary)",display:"block",marginBottom:8,fontWeight:500}}>Orchestration strategy</label>
              <div style={{display:"flex",flexDirection:"column",gap:6}}>
                {STRATEGIES.map(s=>(
                  <label key={s.id} style={{display:"flex",alignItems:"center",gap:8,cursor:"pointer",padding:"8px 10px",borderRadius:6,background:strategy===s.id?"var(--color-background-secondary)":"transparent",border:`0.5px solid ${strategy===s.id?"var(--color-border-secondary)":"transparent"}`,transition:"all .15s"}}>
                    <input type="radio" name="strategy" value={s.id} checked={strategy===s.id} onChange={()=>setStrategy(s.id)} style={{accentColor:"var(--color-text-primary)"}}/>
                    <div>
                      <span style={{fontSize:13,fontWeight:strategy===s.id?500:400}}>{s.name}</span>
                      <span style={{fontSize:11,color:"var(--color-text-secondary)",marginLeft:8}}>{s.desc}</span>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            {/* Settings */}
            <div style={{background:"var(--color-background-primary)",border:"0.5px solid var(--color-border-tertiary)",borderRadius:10,padding:"1rem"}}>
              <label style={{fontSize:12,color:"var(--color-text-secondary)",display:"block",marginBottom:12,fontWeight:500}}>Settings</label>
              <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10}}>
                {[
                  {label:"Max steps",val:maxSteps,set:setMaxSteps,min:1,max:50,unit:""},
                  {label:"Timeout (s)",val:timeout,set:setTimeout,min:10,max:300,unit:"s"},
                ].map(({label,val,set,min,max})=>(
                  <div key={label}>
                    <label style={{fontSize:11,color:"var(--color-text-secondary)",display:"block",marginBottom:4}}>{label} <b style={{fontWeight:500,color:"var(--color-text-primary)"}}>{val}</b></label>
                    <input type="range" min={min} max={max} value={val} step={1} onChange={e=>set(Number(e.target.value))} style={{width:"100%"}}/>
                  </div>
                ))}
              </div>
              <div style={{marginTop:12}}>
                <label style={{fontSize:11,color:"var(--color-text-secondary)",display:"block",marginBottom:4}}>Tenant ID</label>
                <input value={tenantId} onChange={e=>setTenantId(e.target.value)} style={{width:"100%",padding:"6px 8px",borderRadius:6,border:"0.5px solid var(--color-border-tertiary)",fontSize:12,background:"var(--color-background-secondary)",color:"var(--color-text-primary)",boxSizing:"border-box"}}/>
              </div>
              <div style={{marginTop:10}}>
                <label style={{fontSize:11,color:"var(--color-text-secondary)",display:"block",marginBottom:4}}>Default model</label>
                <select value={modelId} onChange={e=>setModelId(e.target.value)} style={{width:"100%",padding:"6px 8px",borderRadius:6,border:"0.5px solid var(--color-border-tertiary)",fontSize:12,background:"var(--color-background-secondary)",color:"var(--color-text-primary)"}}>
                  {["Qwen/Qwen2.5-7B-Instruct","Qwen/Qwen2.5-0.5B-Instruct","meta-llama/Llama-3.2-3B","mistralai/Mistral-7B-Instruct-v0.3","google/gemma-2b-it"].map(m=><option key={m} value={m}>{m}</option>)}
                </select>
              </div>
              <div style={{display:"flex",gap:16,marginTop:12}}>
                {[{label:"Streaming",val:streaming,set:setStreaming},{label:"Verbose",val:verbose,set:setVerbose}].map(({label,val,set})=>(
                  <label key={label} style={{display:"flex",alignItems:"center",gap:6,fontSize:12,cursor:"pointer"}}>
                    <input type="checkbox" checked={val} onChange={e=>set(e.target.checked)} style={{accentColor:"var(--color-text-primary)"}}/>
                    {label}
                  </label>
                ))}
              </div>
            </div>
          </div>

          {/* Right col — selected skills + pipeline preview */}
          <div style={{display:"flex",flexDirection:"column",gap:12}}>
            {/* Skills panel */}
            <div style={{background:"var(--color-background-primary)",border:"0.5px solid var(--color-border-tertiary)",borderRadius:10,padding:"1rem",flex:1}}>
              <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",marginBottom:8}}>
                <label style={{fontSize:12,color:"var(--color-text-secondary)",fontWeight:500}}>Selected skills <span style={{background:"#EEEDFE",color:"#3C3489",padding:"2px 7px",borderRadius:4,fontSize:11,fontWeight:500,marginLeft:4}}>{selectedSkills.length}</span></label>
                <button onClick={()=>setTab("skills")} style={{fontSize:11,padding:"3px 8px",borderRadius:4,border:"0.5px solid var(--color-border-tertiary)",background:"transparent",color:"var(--color-text-secondary)",cursor:"pointer"}}>+ add skills</button>
              </div>
              <div style={{display:"flex",flexDirection:"column",gap:6,minHeight:80}}>
                {selectedSkillData.length === 0 && (
                  <div style={{color:"var(--color-text-secondary)",fontSize:12,padding:"1rem",textAlign:"center",border:"0.5px dashed var(--color-border-tertiary)",borderRadius:6}}>No skills selected — go to Skills tab to add</div>
                )}
                {selectedSkillData.map(s=>{
                  const pal = PALETTES[s.color];
                  return (
                    <div key={s.id} style={{display:"flex",alignItems:"center",justifyContent:"space-between",padding:"8px 10px",borderRadius:7,background:"var(--color-background-secondary)",border:"0.5px solid var(--color-border-tertiary)"}}>
                      <div style={{display:"flex",alignItems:"center",gap:8}}>
                        <div style={{width:22,height:22,borderRadius:5,background:pal.bg,border:`0.5px solid ${pal.border}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:11,color:pal.text}}>⬡</div>
                        <div>
                          <div style={{fontSize:12,fontWeight:500}}>{s.name}</div>
                          <div style={{fontSize:10,color:"var(--color-text-secondary)"}}>{s.category}</div>
                        </div>
                      </div>
                      <button onClick={()=>toggleSkill(s.id)} style={{fontSize:11,padding:"2px 7px",borderRadius:4,border:"0.5px solid var(--color-border-tertiary)",background:"transparent",color:"var(--color-text-secondary)",cursor:"pointer"}}>×</button>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Pipeline visualization */}
            <div style={{background:"var(--color-background-primary)",border:"0.5px solid var(--color-border-tertiary)",borderRadius:10,padding:"1rem"}}>
              <label style={{fontSize:12,color:"var(--color-text-secondary)",display:"block",marginBottom:10,fontWeight:500}}>Agent pipeline</label>
              <svg width="100%" viewBox={`0 0 340 ${80 + selectedSkills.length * 46}`} style={{overflow:"visible"}}>
                <defs>
                  <marker id="arr2" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                    <path d="M2 1L8 5L2 9" fill="none" stroke="context-stroke" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                  </marker>
                </defs>
                {/* Request node */}
                <rect x="60" y="10" width="220" height="36" rx="7" fill="#EEEDFE" stroke="#7F77DD" strokeWidth="0.5"/>
                <text x="170" y="32" textAnchor="middle" style={{fontSize:12,fill:"#3C3489",fontWeight:500,fontFamily:"var(--font-sans)"}}>Agent request</text>
                {/* Strategy */}
                <line x1="170" y1="46" x2="170" y2="60" stroke="var(--color-border-secondary)" strokeWidth="1" markerEnd="url(#arr2)"/>
                <rect x="100" y="60" width="140" height="28" rx="5" fill="var(--color-background-secondary)" stroke="var(--color-border-secondary)" strokeWidth="0.5"/>
                <text x="170" y="78" textAnchor="middle" style={{fontSize:11,fill:"var(--color-text-secondary)",fontFamily:"var(--font-sans)"}}>{STRATEGIES.find(s=>s.id===strategy)?.name} orchestrator</text>
                {/* Skills */}
                {selectedSkillData.map((s,i)=>{
                  const pal = PALETTES[s.color];
                  const y = 108 + i * 46;
                  return (
                    <g key={s.id}>
                      <line x1="170" y1={y-12} x2="170" y2={y} stroke="var(--color-border-secondary)" strokeWidth="1" markerEnd="url(#arr2)" strokeDasharray="3 2"/>
                      <rect x="50" y={y} width="240" height="32" rx="6" fill={pal.bg} stroke={pal.border} strokeWidth="0.5"/>
                      <text x="80" y={y+20} style={{fontSize:11,fill:pal.text,fontWeight:500,fontFamily:"var(--font-sans)"}}>{s.name}</text>
                      <text x="290" y={y+20} textAnchor="end" style={{fontSize:10,fill:pal.text,opacity:.7,fontFamily:"var(--font-sans)"}}>{s.category}</text>
                    </g>
                  );
                })}
                {selectedSkillData.length === 0 && (
                  <text x="170" y="130" textAnchor="middle" style={{fontSize:11,fill:"var(--color-text-secondary)",fontFamily:"var(--font-sans)"}}>No skills — select from Skills tab</text>
                )}
              </svg>
            </div>
          </div>
        </div>
      )}

      {/* SKILLS TAB */}
      {tab === "skills" && (
        <div>
          <div style={{display:"flex",gap:6,marginBottom:14,flexWrap:"wrap"}}>
            {categories.map(c=>(
              <button key={c} onClick={()=>setCatFilter(c)} style={{
                fontSize:11,padding:"4px 10px",borderRadius:5,
                border:`0.5px solid ${catFilter===c?"var(--color-border-primary)":"var(--color-border-tertiary)"}`,
                background:catFilter===c?"var(--color-background-secondary)":"transparent",
                color:catFilter===c?"var(--color-text-primary)":"var(--color-text-secondary)",cursor:"pointer",fontWeight:catFilter===c?500:400
              }}>{c}</button>
            ))}
          </div>
          <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fill,minmax(200px,1fr))",gap:10}}>
            {filteredSkills.map(s=>{
              const pal = PALETTES[s.color];
              const sel = selectedSkills.includes(s.id);
              return (
                <div key={s.id} onClick={()=>toggleSkill(s.id)} style={{
                  padding:"12px",borderRadius:10,cursor:"pointer",
                  background:sel?pal.bg:"var(--color-background-primary)",
                  border:`${sel?"2px":"0.5px"} solid ${sel?pal.border:"var(--color-border-tertiary)"}`,
                  transition:"all .15s",position:"relative"
                }}>
                  {sel && <div style={{position:"absolute",top:8,right:8,width:14,height:14,borderRadius:"50%",background:pal.border,display:"flex",alignItems:"center",justifyContent:"center",fontSize:9,color:"#fff",fontWeight:700}}>✓</div>}
                  <div style={{width:28,height:28,borderRadius:6,background:sel?"#fff":pal.bg,border:`0.5px solid ${pal.border}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:14,marginBottom:8,color:pal.text}}>⬡</div>
                  <div style={{fontSize:13,fontWeight:500,marginBottom:3,color:sel?pal.text:"var(--color-text-primary)"}}>{s.name}</div>
                  <div style={{fontSize:10,color:sel?pal.text:"var(--color-text-secondary)",opacity:0.8,marginBottom:6,lineHeight:1.4}}>{s.description}</div>
                  <span style={{fontSize:10,padding:"2px 6px",borderRadius:3,background:pal.bg,border:`0.5px solid ${pal.border}`,color:pal.text}}>{s.category}</span>
                </div>
              );
            })}
          </div>
          <div style={{marginTop:14,padding:"10px 14px",borderRadius:8,background:"var(--color-background-secondary)",border:"0.5px solid var(--color-border-tertiary)",fontSize:12,color:"var(--color-text-secondary)"}}>
            <b style={{fontWeight:500,color:"var(--color-text-primary)"}}>{selectedSkills.length} skills selected:</b> {selectedSkills.join(", ") || "none"}
          </div>
        </div>
      )}

      {/* CODE TAB */}
      {tab === "code" && (
        <div style={{display:"flex",flexDirection:"column",gap:14}}>
          <div style={{display:"flex",gap:0,borderBottom:"0.5px solid var(--color-border-tertiary)"}}>
            {["java","curl","yaml"].map(t=>(
              <button key={t} onClick={()=>setCodeTab(t)} style={{
                padding:"6px 14px",fontSize:12,fontWeight:codeTab===t?500:400,
                background:"transparent",border:"none",borderBottom:codeTab===t?"2px solid var(--color-text-primary)":"2px solid transparent",
                color:codeTab===t?"var(--color-text-primary)":"var(--color-text-secondary)",cursor:"pointer",textTransform:"uppercase",letterSpacing:"0.05em"
              }}>{t}</button>
            ))}
          </div>
          <div style={{borderRadius:10,overflow:"hidden",border:"0.5px solid var(--color-border-tertiary)"}}>
            <div style={{background:"var(--color-background-secondary)",padding:"6px 12px",borderBottom:"0.5px solid var(--color-border-tertiary)",fontSize:11,color:"var(--color-text-secondary)",display:"flex",justifyContent:"space-between",alignItems:"center"}}>
              <span>{codeTab === "java" ? "Java (Quarkus CDI)" : codeTab === "curl" ? "REST API (curl)" : "application.yaml"}</span>
              <button onClick={()=>navigator.clipboard?.writeText(codeTab==="java"?generateCode():codeTab==="curl"?generateCurl():generateYaml())} style={{fontSize:11,padding:"2px 8px",borderRadius:4,border:"0.5px solid var(--color-border-tertiary)",background:"transparent",color:"var(--color-text-secondary)",cursor:"pointer"}}>copy</button>
            </div>
            <pre style={{margin:0,padding:"14px 16px",fontSize:11,lineHeight:1.7,overflow:"auto",background:"var(--color-background-primary)",color:"var(--color-text-primary)",fontFamily:"var(--font-mono)",whiteSpace:"pre-wrap",wordBreak:"break-word"}}>
              {codeTab==="java"?generateCode():codeTab==="curl"?generateCurl():generateYaml()}
            </pre>
          </div>
          <div style={{display:"grid",gridTemplateColumns:"repeat(3,1fr)",gap:10}}>
            {[
              {label:"Skills",value:selectedSkills.length,unit:""},
              {label:"Max steps",value:maxSteps,unit:""},
              {label:"Timeout",value:timeout,unit:"s"},
            ].map(({label,value,unit})=>(
              <div key={label} style={{padding:"12px",borderRadius:8,background:"var(--color-background-secondary)",border:"0.5px solid var(--color-border-tertiary)",textAlign:"center"}}>
                <div style={{fontSize:22,fontWeight:500}}>{value}{unit}</div>
                <div style={{fontSize:12,color:"var(--color-text-secondary)",marginTop:2}}>{label}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ARCHITECTURE TAB */}
      {tab === "architecture" && (
        <div style={{display:"flex",flexDirection:"column",gap:16}}>
          <p style={{fontSize:13,color:"var(--color-text-secondary)",margin:0,lineHeight:1.6}}>
            The Gollek Agent System follows a skills-based agentic architecture. Each capability is a versioned, discoverable <b style={{fontWeight:500}}>AgentSkill</b> SPI implementation. The orchestrator drives a reasoning loop, calling skills as tools and building toward a final answer.
          </p>
          <svg width="100%" viewBox="0 0 680 480">
            <defs>
              <marker id="arr3" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
                <path d="M2 1L8 5L2 9" fill="none" stroke="context-stroke" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </marker>
            </defs>
            {/* REST layer */}
            <rect x="40" y="20" width="600" height="50" rx="8" fill="#EEEDFE" stroke="#7F77DD" strokeWidth="0.5"/>
            <text x="340" y="38" textAnchor="middle" style={{fontSize:12,fill:"#3C3489",fontWeight:500,fontFamily:"var(--font-sans)"}}>REST API  /api/v1/agents</text>
            <text x="340" y="56" textAnchor="middle" style={{fontSize:10,fill:"#534AB7",fontFamily:"var(--font-sans)"}}>AgentResource  — run, stream, skills, health</text>
            {/* Arrow down */}
            <line x1="340" y1="70" x2="340" y2="92" stroke="var(--color-border-secondary)" strokeWidth="1" markerEnd="url(#arr3)"/>
            {/* AgentBuilder */}
            <rect x="150" y="92" width="380" height="44" rx="8" fill="#E1F5EE" stroke="#1D9E75" strokeWidth="0.5"/>
            <text x="340" y="111" textAnchor="middle" style={{fontSize:12,fill:"#085041",fontWeight:500,fontFamily:"var(--font-sans)"}}>AgentBuilder  (fluent DSL)</text>
            <text x="340" y="127" textAnchor="middle" style={{fontSize:10,fill:"#0F6E56",fontFamily:"var(--font-sans)"}}>withPrompt · usingSkills · withModel · forTenant · execute</text>
            {/* Arrow down */}
            <line x1="340" y1="136" x2="340" y2="158" stroke="var(--color-border-secondary)" strokeWidth="1" markerEnd="url(#arr3)"/>
            {/* Orchestrators */}
            <rect x="40" y="158" width="600" height="62" rx="8" fill="#FAEEDA" stroke="#BA7517" strokeWidth="0.5"/>
            <text x="340" y="176" textAnchor="middle" style={{fontSize:12,fill:"#633806",fontWeight:500,fontFamily:"var(--font-sans)"}}>Orchestrators</text>
            <rect x="60" y="185" width="120" height="26" rx="5" fill="#fff" stroke="#BA7517" strokeWidth="0.5"/>
            <text x="120" y="202" textAnchor="middle" style={{fontSize:10,fill:"#633806",fontFamily:"var(--font-sans)"}}>ReAct</text>
            <rect x="200" y="185" width="140" height="26" rx="5" fill="#fff" stroke="#BA7517" strokeWidth="0.5"/>
            <text x="270" y="202" textAnchor="middle" style={{fontSize:10,fill:"#633806",fontFamily:"var(--font-sans)"}}>Plan & Execute</text>
            <rect x="360" y="185" width="130" height="26" rx="5" fill="#fff" stroke="#BA7517" strokeWidth="0.5"/>
            <text x="425" y="202" textAnchor="middle" style={{fontSize:10,fill:"#633806",fontFamily:"var(--font-sans)"}}>Chain of Thought</text>
            <rect x="510" y="185" width="110" height="26" rx="5" fill="#fff" stroke="#BA7517" strokeWidth="0.5"/>
            <text x="565" y="202" textAnchor="middle" style={{fontSize:10,fill:"#633806",fontFamily:"var(--font-sans)"}}>Reflexion</text>
            {/* Arrow down */}
            <line x1="340" y1="220" x2="340" y2="242" stroke="var(--color-border-secondary)" strokeWidth="1" markerEnd="url(#arr3)"/>
            {/* Skill Registry */}
            <rect x="120" y="242" width="240" height="44" rx="8" fill="#E6F1FB" stroke="#378ADD" strokeWidth="0.5"/>
            <text x="240" y="261" textAnchor="middle" style={{fontSize:12,fill:"#0C447C",fontWeight:500,fontFamily:"var(--font-sans)"}}>Skill Registry</text>
            <text x="240" y="277" textAnchor="middle" style={{fontSize:10,fill:"#185FA5",fontFamily:"var(--font-sans)"}}>CDI + ServiceLoader discovery</text>
            {/* Memory */}
            <rect x="400" y="242" width="240" height="44" rx="8" fill="#FBEAF0" stroke="#D4537E" strokeWidth="0.5"/>
            <text x="520" y="261" textAnchor="middle" style={{fontSize:12,fill:"#4B1528",fontWeight:500,fontFamily:"var(--font-sans)"}}>Agent Memory</text>
            <text x="520" y="277" textAnchor="middle" style={{fontSize:10,fill:"#72243E",fontFamily:"var(--font-sans)"}}>conversation · vector · working</text>
            {/* Arrow down */}
            <line x1="240" y1="286" x2="240" y2="310" stroke="var(--color-border-secondary)" strokeWidth="1" markerEnd="url(#arr3)"/>
            {/* Skills row */}
            <text x="340" y="304" textAnchor="middle" style={{fontSize:10,fill:"var(--color-text-secondary)",fontFamily:"var(--font-sans)"}}>Built-in skills (extensible via SPI)</text>
            {[
              {x:40,label:"Inference",c:"#EEEDFE",s:"#7F77DD",t:"#3C3489"},
              {x:152,label:"RAG",c:"#E1F5EE",s:"#1D9E75",t:"#085041"},
              {x:232,label:"Code Exec",c:"#FAEEDA",s:"#BA7517",t:"#633806"},
              {x:332,label:"Web Search",c:"#E6F1FB",s:"#378ADD",t:"#0C447C"},
              {x:442,label:"Data Analysis",c:"#EAF3DE",s:"#639922",t:"#27500A"},
              {x:562,label:"Custom…",c:"#F1EFE8",s:"#888780",t:"#2C2C2A"},
            ].map(({x,label,c,s,t})=>(
              <g key={label}>
                <rect x={x} y="314" width={label==="Custom…"?78:label.length*8+16} height="32" rx="6" fill={c} stroke={s} strokeWidth="0.5"/>
                <text x={x+(label==="Custom…"?78:label.length*8+16)/2} y="334" textAnchor="middle" style={{fontSize:10,fill:t,fontFamily:"var(--font-sans)"}}>{label}</text>
              </g>
            ))}
            {/* Arrow down */}
            <line x1="340" y1="346" x2="340" y2="368" stroke="var(--color-border-secondary)" strokeWidth="1" markerEnd="url(#arr3)"/>
            {/* Gollek Engine */}
            <rect x="40" y="368" width="600" height="50" rx="8" fill="#EAF3DE" stroke="#639922" strokeWidth="0.5"/>
            <text x="340" y="388" textAnchor="middle" style={{fontSize:12,fill:"#27500A",fontWeight:500,fontFamily:"var(--font-sans)"}}>Gollek Inference Engine</text>
            <text x="340" y="406" textAnchor="middle" style={{fontSize:10,fill:"#3B6D11",fontFamily:"var(--font-sans)"}}>GGUF · SafeTensors · LibTorch · ONNX · TFLite · Remote providers</text>
            {/* labels on sides */}
            <text x="20" y="48" textAnchor="start" style={{fontSize:9,fill:"var(--color-text-secondary)",fontFamily:"var(--font-sans)"}}>HTTP</text>
            <text x="20" y="220" textAnchor="start" style={{fontSize:9,fill:"var(--color-text-secondary)",fontFamily:"var(--font-sans)"}}>CDI</text>
            <text x="20" y="395" textAnchor="start" style={{fontSize:9,fill:"var(--color-text-secondary)",fontFamily:"var(--font-sans)"}}>JVM</text>
          </svg>
        </div>
      )}

      {/* Footer summary bar */}
      <div style={{marginTop:18,padding:"10px 14px",borderRadius:8,background:"var(--color-background-secondary)",border:"0.5px solid var(--color-border-tertiary)",display:"flex",gap:16,flexWrap:"wrap",alignItems:"center",fontSize:12}}>
        <span style={{color:"var(--color-text-secondary)"}}>Agent summary:</span>
        <span><b style={{fontWeight:500}}>{STRATEGIES.find(s=>s.id===strategy)?.name}</b></span>
        <span style={{color:"var(--color-border-secondary)"}}>·</span>
        <span><b style={{fontWeight:500}}>{selectedSkills.length}</b> skills</span>
        <span style={{color:"var(--color-border-secondary)"}}>·</span>
        <span>max <b style={{fontWeight:500}}>{maxSteps}</b> steps</span>
        <span style={{color:"var(--color-border-secondary)"}}>·</span>
        <span>tenant <b style={{fontWeight:500}}>{tenantId}</b></span>
        <span style={{color:"var(--color-border-secondary)"}}>·</span>
        <span style={{color:"var(--color-text-secondary)"}}>{modelId.split("/")[1] || modelId}</span>
        {streaming && <><span style={{color:"var(--color-border-secondary)"}}>·</span><span style={{color:"#1D9E75",fontWeight:500}}>streaming</span></>}
      </div>
    </div>
  );
}
