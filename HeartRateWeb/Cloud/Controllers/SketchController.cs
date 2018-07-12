using Microsoft.AspNetCore.Mvc;

namespace Cloud.Controllers
{
    public class SketchController : Controller
    {
        public IActionResult Index()
        {
            return View();
        }
    }
}
