
let projects = [];
var hostURL = "http://localhost:7000"


function fetchProjects() {
    fetch(hostURL + "/projects")
        .then(response => response.json())
        .then(data => {
            projects = data;
            renderProjectList();
        });
}

function addProject(name) {
    if (!name || !name.trim()) return;
    fetch(hostURL + "/projects", {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: name.trim() })
    })
    .then(response => {
        console.log(response)
        if (!response.ok) {
            return response.text().then(msg => { throw new Error(msg); });
        }
        return response.json ? response.json() : response.text();
    })
    .then(() => {
        fetchProjects();
    })
    .catch(err => {
        alert('Project couldnt be created: ' + err.message);
    });
}

function deleteProject(name){
    if(!name){
        return;
    }
    fetch(hostURL + "/projects/" + name + "/del", {
        method: "GET"
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Failed to delete project");
        }
        return response.text();
    })
    .then(() => {
        fetchProjects();
    })
    .catch(err => {
        alert('Project couldnt be deleted: ' + err.message);
    });
}


function renderProjectList() {
    const projectList = document.getElementById("project-list");
    projectList.innerHTML = "";

    //Add new projects
    const addDiv = document.createElement('li');
    addDiv.className = 'project-add-row';
    addDiv.style.display = 'flex';
    addDiv.style.alignItems = 'center';
    addDiv.style.gap = '8px';
    const input = document.createElement('input');
    input.type = 'text';
    input.placeholder = 'Add Project...';
    input.className = 'add-project-input';
    const addBtn = document.createElement('button');
    addBtn.textContent = 'Add';
    addBtn.className = 'add-project-btn';
    addBtn.onclick = function() {
        addProject(input.value);
        input.value = '';
    };
    input.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            addProject(input.value);
            input.value = '';
        }
    });
    addDiv.appendChild(input);
    addDiv.appendChild(addBtn);
    projectList.appendChild(addDiv);

    //List existing projects
    projects.forEach(project => {
        const li = document.createElement("li");
        li.className = "project-list-item";
        const nameSpan = document.createElement('span');
        nameSpan.textContent = project;
        nameSpan.className = "project-name";
        const selectBtn = document.createElement('button');
        selectBtn.textContent = 'Select';
        selectBtn.className = 'select-project-btn';
        selectBtn.onclick = function() {
            window.location.href = `index.html?project=${encodeURIComponent(project)}`;
        };
        const delBtn = document.createElement('button');
        delBtn.textContent = 'Delete';
        delBtn.className = 'delete-project-btn';
        delBtn.onclick = function() {
            if (confirm("Are you sure you want to delete this project?")) {
                deleteProject(project);
            }
        };
        li.appendChild(nameSpan);
        li.appendChild(selectBtn);
        li.appendChild(delBtn);
        projectList.appendChild(li);
    });
}

// Initiales Laden der Projekte
fetchProjects();
