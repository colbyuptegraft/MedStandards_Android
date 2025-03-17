var pdfDoc = null;

function createPage() {
    var canvas = document.createElement("canvas");
    canvas.style.display = "block"; // Уникнення зайвих пробілів
    canvas.style.margin = "auto"; // Центрування

    document.body.appendChild(canvas);
    return canvas;
}

function renderPage(num) {
    pdfDoc.getPage(num).then(function (page) {
        var screenWidth = window.innerWidth; // Отримуємо ширину екрану
        var viewport = page.getViewport({ scale: 1 }); // Масштаб 1 для отримання розмірів

        var scale = screenWidth / viewport.width; // Динамічний масштаб для підлаштування під ширину екрана
        viewport = page.getViewport({ scale: scale });

        var canvas = createPage();
        var ctx = canvas.getContext('2d');

        var outputScale = Math.min(window.devicePixelRatio || 1, 2); // Обмежуємо до 2x
        canvas.width = Math.floor(viewport.width * outputScale);
        canvas.height = Math.floor(viewport.height * outputScale);
        canvas.style.width = Math.floor(viewport.width) + "px";
        canvas.style.height = Math.floor(viewport.height) + "px";

        var renderContext = {
            canvasContext: ctx,
            viewport: viewport,
            transform: [outputScale, 0, 0, outputScale, 0, 0] // Чіткість тексту
        };

        page.render(renderContext).promise.then(() => {
            console.log(`Page ${num} rendered at ${outputScale}x scale`);
        });
    });
}




function scrollToPage(pageIndex) {
    const canvasList = document.getElementsByTagName("canvas");

    if (pageIndex < 0 || pageIndex >= canvasList.length) return;

    const targetCanvas = canvasList[pageIndex];

    // Використовуємо getBoundingClientRect(), щоб отримати точну позицію
    const topOffset = targetCanvas.getBoundingClientRect().top + window.scrollY;

    window.scrollTo({
        top: topOffset,
        behavior: "smooth"
    });
}



function scrollToWord(x, y) {
    window.scrollTo({
        left: x,
        top: y,
        behavior: "smooth"
    });
}

function receivePDF(base64String) {
    console.log("Отримано Base64 PDF");

    try {
        var binaryString = atob(base64String);
        var bytes = new Uint8Array(binaryString.length);
        for (var i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        pdfjsLib.getDocument({ data: bytes.buffer }).promise.then(function (pdf) {
            pdfDoc = pdf;
            document.body.innerHTML = ""; // Очищення перед рендерингом

            for (var i = 1; i <= pdfDoc.numPages; i++) {
                renderPage(i);
            }
        }).catch(function (error) {
            console.error("Помилка завантаження PDF:", error);
        });

    } catch (e) {
        console.error("Помилка обробки Base64:", e);
    }
}
